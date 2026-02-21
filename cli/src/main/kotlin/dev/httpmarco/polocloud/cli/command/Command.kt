package dev.httpmarco.polocloud.cli.command

import dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument

abstract class Command(val name: String, val description: String, vararg val aliases: String) {

    var  defaultExecution: CommandExecution? = null
    val commandSyntaxes = ArrayList<CommandSyntax>()

    fun syntax(execution: CommandExecution, vararg arguments: TerminalArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, null, arguments.toList() as MutableList<TerminalArgument<*>>))
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: TerminalArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, description, arguments.toList() as MutableList<TerminalArgument<*>>))
    }

    fun defaultExecution(execution: CommandExecution) {
        this.defaultExecution = execution
    }
}