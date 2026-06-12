package de.polocloud.common.commands

import de.polocloud.common.commands.type.KeywordArgument

class CommandSyntax(
    val execution: CommandExecution,
    val description: String?,
    val arguments: MutableList<TerminalArgument<*>>
) {
    fun usage(): String {
        return java.lang.String.join(
            " ", arguments.stream()
                .map { if (it is KeywordArgument) "&f" + it.key else "&8<&f" + it.key + "&8>" }
                .toList()
        ) + (if (description == null) "" else " &8- &7$description")
    }
}