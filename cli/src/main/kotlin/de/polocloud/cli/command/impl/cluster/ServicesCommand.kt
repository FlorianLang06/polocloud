package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import kotlinx.coroutines.runBlocking

class ServicesCommand(
    private val connectionManager: CliConnectionManager
) : Command("services", "List all services in the cluster") {//TODO

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info("Not connected.")
                    return@syntax
                }

                runBlocking {
                    val services = Cli.session.serviceClient.listServices()

                    if (services.isEmpty()) {
                        logger.info("No services found.")
                        return@runBlocking
                    }

                    logger.info("STATE      PLAN            UUID                                 PORT     PID    PLAYERS")
                    services.forEach { service ->
                        logger.info(
                            "%-10s %-15s %-36s %-8s %-6s %s/%s".format(
                                service.state,
                                service.plan,
                                service.uuid,
                                service.boundPort,
                                service.pid,
                                service.onlinePlayers,
                                service.maxPlayers,
                            )
                        )
                    }
                }
            },
            KeywordArgument("list")
        )
    }
}