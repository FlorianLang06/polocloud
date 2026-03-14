package de.polocloud.cli.command.impl

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command

class ClearCommand : Command("clear", "Clears the terminal screen") {

    init {
        defaultExecution {
            Cli.terminal.clearScreen()
        }
    }
}