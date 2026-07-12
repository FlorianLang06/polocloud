package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.node.terminal.CliTerminal

/**
 * Clears the terminal screen.
 */
class ClearCommand(
    private val terminal: CliTerminal,
) : Command("clear", "Clears the console screen", "cls") {

    init {
        defaultExecution {
            terminal.clearScreen()
        }
    }
}