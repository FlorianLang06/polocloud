package dev.httpmarco.polocloud.cli.command

import dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument
import dev.httpmarco.polocloud.cli.command.arguments.type.KeywordArgument

/**
 * Represents a specific argument combination (syntax variant) for a [Command].
 *
 * Each syntax maps a list of [arguments] to an [execution] block and an optional [description]
 * shown in the help output. [KeywordArgument] entries are rendered as plain text in the usage
 * string, while other argument types are wrapped in angle brackets.
 *
 * @param execution The action to invoke when this syntax is matched.
 * @param description Optional description shown alongside the usage line in help output.
 * @param arguments The ordered list of arguments expected by this syntax.
 */
class CommandSyntax(
    val execution: CommandExecution,
    val description: String?,
    val arguments: MutableList<TerminalArgument<*>>
) {

    /**
     * Builds a human-readable usage string for this syntax.
     *
     * Keyword arguments are shown as plain text; all others are shown as `<key>`.
     * The optional [description] is appended after a separator if present.
     *
     * @return A formatted usage string, e.g. `"group create <name> - Creates a new group"`.
     */
    fun usage(): String {
        return java.lang.String.join(
            " ", arguments.stream()
                .map { if (it is KeywordArgument) "&f" + it.key else "&8<&f" + it.key + "&8>" }
                .toList()
        ) + (if (description == null) "" else " &8- &7$description")
    }
}