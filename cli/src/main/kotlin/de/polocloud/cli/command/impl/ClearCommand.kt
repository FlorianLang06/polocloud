package de.polocloud.cli.command.impl

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.i18n.api.TranslationService

class ClearCommand : Command("clear","cli.command.impl.clear.description") {

    init {
        defaultExecution {
            Cli.terminal.clearScreen()
        }
    }
}