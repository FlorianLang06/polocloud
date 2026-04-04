package de.polocloud.cli.command.impl

import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.command.arguments.type.int.IntArgument
import de.polocloud.cli.command.arguments.type.string.TextArgument
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.common.Address
import kotlinx.coroutines.runBlocking

/**
 * Connects the CLI to a cluster.
 *
 * Syntaxes:
 *   connect <host> <clusterPort> <registrationPort> <token>
 *       — registers on the plaintext registration port, then connects via mTLS on the cluster port
 *   connect disconnect
 *       — closes the active mTLS connection
 *
 * Example:
 *   connect 127.0.0.1 4239 4240 my-token
 */
class ConnectCommand(
    private val connectionManager: CliConnectionManager
) : Command("connect", "cli.command.impl.connect.description") {

    private val hostArg             = TextArgument("host")
    private val clusterPortArg      = IntArgument("clusterPort",      minValue = 1, maxValue = 65535)
    private val registrationPortArg = IntArgument("registrationPort", minValue = 1, maxValue = 65535)
    private val tokenArg            = TextArgument("token")

    init {
        syntax(
            { ctx ->
                val host             = ctx.arg(hostArg)
                val clusterPort      = ctx.arg(clusterPortArg)
                val registrationPort = ctx.arg(registrationPortArg)
                val token            = ctx.arg(tokenArg)

                if (connectionManager.isConnected) {
                    logger.info("&cAlready connected. Run &fconnect disconnect &cfirst.")
                    return@syntax
                }

                logger.info("&7Connecting to cluster at &f$host&7 (cluster: &f$clusterPort&7, registration: &f$registrationPort&7)...")

                runBlocking {
                    runCatching {
                        connectionManager.connect(
                            clusterAddress      = Address(host, clusterPort),
                            registrationAddress = Address(host, registrationPort),
                            token               = token
                        )
                    }.onSuccess {
                        logger.info("&aSuccessfully connected to cluster at &f$host:$clusterPort&a.")
                    }.onFailure { ex ->
                        logger.info("&cFailed to connect: &f${ex.message}")
                    }
                }
            },
            "Connect to a cluster node",
            hostArg, clusterPortArg, registrationPortArg, tokenArg
        )

        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info("&cNot connected to any cluster.")
                    return@syntax
                }
                connectionManager.disconnect()
                logger.info("&aDisconnected from cluster.")
            },
            "Disconnect from the current cluster",
            KeywordArgument("disconnect")
        )
    }
}