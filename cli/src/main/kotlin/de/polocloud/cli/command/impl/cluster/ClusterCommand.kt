package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

class ClusterCommand(
    private val connectionManager: CliConnectionManager
) : Command("cluster", "Cluster Commands") {

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info("Not connected.")
                    return@syntax
                }

                runBlocking {
                    val token = Cli.session.clusterClient.createToken()

                    val time = Instant.ofEpochMilli(token.expiresAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    logger.info("Token: ${token.token} available until $time")
                }
            },
            KeywordArgument("connect")
        )
    }
}