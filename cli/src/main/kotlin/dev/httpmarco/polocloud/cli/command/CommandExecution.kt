package dev.httpmarco.polocloud.cli.command

import dev.httpmarco.polocloud.cli.command.arguments.InputContext

fun interface CommandExecution {
    fun execute(inputContext: InputContext)
}