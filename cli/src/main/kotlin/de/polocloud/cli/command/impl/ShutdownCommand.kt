package de.polocloud.cli.command.impl

import de.polocloud.cli.command.Command
import de.polocloud.cli.exitPolocloud

/**
 * Shuts down the PoloCloud CLI application.
 *
 * Registered with both the primary name `shutdown` and the alias `stop`.
 * When executed, delegates to [exitPolocloud] for a clean shutdown sequence.
 */
class ShutdownCommand : Command("shutdown", "Shuts down the agent", "stop") {

    init {
        defaultExecution {
            exitPolocloud()
        }
    }
}
