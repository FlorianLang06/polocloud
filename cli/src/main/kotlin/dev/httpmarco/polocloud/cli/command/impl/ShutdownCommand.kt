package dev.httpmarco.polocloud.cli.command.impl

import dev.httpmarco.polocloud.cli.command.Command
import dev.httpmarco.polocloud.cli.exitPolocloud

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
