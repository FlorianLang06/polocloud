package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import kotlin.system.exitProcess

/**
 * Shuts down the node process.
 *
 * Registered with both the primary name `shutdown` and the alias `stop`.
 * Triggers the JVM shutdown hook registered in [de.polocloud.node.core.lifecycle.NodeLifecycle],
 * which performs the graceful shutdown sequence (services, connections, database, ...).
 */
class ShutdownCommand : Command("shutdown", "Shuts down the node", "stop") {

    init {
        defaultExecution {
            exitProcess(0)
        }
    }
}
