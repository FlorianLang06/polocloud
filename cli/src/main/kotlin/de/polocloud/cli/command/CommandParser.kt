package de.polocloud.cli.command

import de.polocloud.cli.command.arguments.InputContext
import de.polocloud.cli.command.arguments.type.StringArrayArgument
import de.polocloud.cli.logger

/**
 * Parses raw terminal input and dispatches it to the correct [Command] and [CommandSyntax].
 *
 * On each [parse] call, the parser:
 * 1. Looks up the command by name via [CommandService].
 * 2. If no arguments are given, invokes the [Command.defaultExecution] or prints help.
 * 3. Otherwise, iterates over all registered syntaxes and finds the first match via argument predication.
 * 4. Builds an [InputContext] with the parsed values and invokes the matched [CommandSyntax.execution].
 *
 * @param commandService The [CommandService] used to resolve commands by name.
 */
class CommandParser(private val commandService: CommandService) {

    /**
     * Parses and dispatches the command identified by [commandId] with the given [args].
     *
     * Prints help output if the command is unknown, no-arg default is missing, or no syntax matches.
     *
     * @param commandId The command name or alias entered by the user.
     * @param args The arguments that followed the command name.
     */
    fun parse(commandId: String, args: Array<String>) {
        val matches = commandService.findByName(commandId)

        if (matches.isEmpty()) {
            return
        }

        val command = matches.first()

        if (args.isEmpty()) {
            command.defaultExecution?.execute(InputContext()) ?: printHelp(command)
            return
        }

        if (matchSyntax(command, args) == null) {
            printHelp(command)
        }
    }

    /**
     * Attempts to find and execute a [CommandSyntax] that matches the given [args].
     *
     * Each syntax is tried in registration order. A syntax matches when every argument passes
     * its [dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument.predication] check. [StringArrayArgument] consumes all remaining
     * tokens as a single joined string.
     *
     * @param command The command whose syntaxes are evaluated.
     * @param args The user-provided argument tokens.
     * @return The matched [CommandSyntax] if one was found and executed, or `null` otherwise.
     */
    private fun matchSyntax(command: Command, args: Array<String>): CommandSyntax? {
        for (syntax in command.syntaxes) {
            val arguments = syntax.arguments
            val lastArgument = arguments.lastOrNull() ?: continue

            // Argument count must match unless the last argument consumes remaining tokens
            if (arguments.size != args.size && lastArgument !is StringArrayArgument) {
                continue
            }

            val context = InputContext()
            var matched = false

            for (i in arguments.indices) {
                val argument = arguments[i]
                val rawInput = args.getOrNull(i) ?: break

                if (!argument.predication(rawInput)) break

                val value = if (argument is StringArrayArgument) {
                    argument.buildResult(args.drop(i).joinToString(" "), context)
                } else {
                    argument.buildResult(rawInput, context)
                }

                context.append(argument, value)

                if (argument == lastArgument) {
                    syntax.execution.execute(context)
                    matched = true
                    break
                }
            }

            if (matched) return syntax
        }

        return null
    }

    /**
     * Logs all registered syntaxes for [command] as help output.
     *
     * @param command The command whose usage lines should be printed.
     */
    private fun printHelp(command: Command) {
        command.syntaxes.forEach { syntax ->
            logger.info(" &8- &7${syntax.usage()}")
        }
    }
}