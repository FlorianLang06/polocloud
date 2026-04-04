package de.polocloud.cli.command

import de.polocloud.cli.command.arguments.TerminalArgument
import de.polocloud.i18n.api.TranslationService

/**
 * Base class for all CLI commands.
 *
 * A command consists of a primary [name], an optional set of [aliases], and a [description].
 * Commands can define multiple syntaxes (argument combinations) via [syntax], and an optional
 * [defaultExecution] that runs when no arguments are provided.
 *
 * Example:
 * ```kotlin
 * class HelpCommand : Command("help", "Shows help information", "?") {
 *     init {
 *         defaultExecution { println("Available commands: ...") }
 *         syntax({ ctx -> println(ctx.arg(topicArg)) }, "Show topic help", topicArg)
 *     }
 * }
 * ```
 *
 * @param name The primary command name used for lookup (case-insensitive).
 * @param descriptionKey Translation key for the command description.
 * @param aliases Optional alternative names for this command.
 */
abstract class Command(
    val name: String,
    val descriptionKey: String,
    vararg val aliases: String
) {

    val description: String
        get() = TranslationService.tr("cli", descriptionKey)

    /**
     * The execution block invoked when the command is called without arguments.
     */
    var defaultExecution: CommandExecution? = null
        private set

    /**
     * All registered syntaxes (argument combinations) for this command.
     */
    val syntaxes = mutableListOf<CommandSyntax>()

    /**
     * Registers a new command syntax with the given [arguments] and [execution] block.
     *
     * @param execution The action to run when this syntax is matched.
     * @param arguments The expected argument sequence for this syntax.
     */
    fun syntax(execution: CommandExecution, vararg arguments: TerminalArgument<*>) {
        syntaxes.add(CommandSyntax(execution, descriptionKey = null, arguments.toMutableList()))
    }

    /**
     * Registers a new command syntax with a [description] shown in help output.
     *
     * @param execution The action to run when this syntax is matched.
     * @param description A short description for this specific syntax variant.
     * @param arguments The expected argument sequence for this syntax.
     */
    fun syntax(execution: CommandExecution, descriptionKey: String, vararg arguments: TerminalArgument<*>) {
        syntaxes.add(CommandSyntax(execution, descriptionKey, arguments.toMutableList()))
    }

    /**
     * Sets the [execution] block to run when the command is called with no arguments.
     *
     * @param execution The action to invoke on a no-argument call.
     */
    fun defaultExecution(execution: CommandExecution) {
        this.defaultExecution = execution
    }
}