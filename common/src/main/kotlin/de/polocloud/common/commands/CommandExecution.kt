package de.polocloud.common.commands

fun interface CommandExecution {
    fun execute(inputContext: InputContext)
}