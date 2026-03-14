package de.polocloud.cli.command.impl

import de.polocloud.cli.command.Command
import de.polocloud.cli.exitPolocloud
import de.polocloud.i18n.api.TranslationService

/**
 * Shuts down the PoloCloud CLI application.
 *
 * Registered with both the primary name `shutdown` and the alias `stop`.
 * When executed, delegates to [exitPolocloud] for a clean shutdown sequence.
 */
class ShutdownCommand : Command("shutdown","cli.command.impl.shutdown.description", "stop") {

    init {
        defaultExecution {
            exitPolocloud() //TODO make check if the user is sure to want to shutdown
            // cli.command.impl.shutdown.confirmation=Are you sure you want to shut down the CLI? (yes/no)
        }
    }
}
