package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.node.NodeEventListener
import de.polocloud.cli.node.NodeClient
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.command.arguments.type.int.IntArgument
import de.polocloud.cli.command.arguments.type.string.TextArgument
import de.polocloud.cli.configuration.connection.ConnectionEntry
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.common.Address
import de.polocloud.i18n.api.TranslationService
import kotlinx.coroutines.runBlocking

class ConnectCommand(
    private val connectionManager: CliConnectionManager
) : Command("connect", "cli.command.impl.connect.description") {

    private val hostArg = TextArgument("host")
    private val clusterPortArg = IntArgument("clusterPort", 1, 65535)
    private val registrationPortArg = IntArgument("registrationPort", 1, 65535)
    private val tokenArg = TextArgument("token")

    //TODO we need better/simpler command and disconnect needs to be better.


    init {
        syntax(
            { ctx ->
                val host = ctx.arg(hostArg)
                val clusterPort = ctx.arg(clusterPortArg)
                val registrationPort = ctx.arg(registrationPortArg)
                val token = ctx.arg(tokenArg)

                if (connectionManager.isConnected) {
                    logger.info(TranslationService.tr("cli", "cli.connect.alreadyConnected"))
                    return@syntax
                }

                logger.info(
                    TranslationService.tr(
                    "cli",
                    "cli.connect.connecting",
                    "host" to host,
                    "clusterPort" to clusterPort,
                    "registrationPort" to registrationPort
                ))

                runBlocking {
                    runCatching {
                        connectionManager.connect(
                            clusterAddress = Address(host, clusterPort),
                            registrationAddress = Address(host, registrationPort),
                            token = token
                        )
                    }.onSuccess {
                        logger.info(
                            TranslationService.tr(
                                "cli",
                                "cli.connect.success",
                                "host" to host,
                                "port" to clusterPort
                            )
                        )

                        Cli.connectionHistory.push(
                            ConnectionEntry(
                                clusterAddress = Address(host, clusterPort),
                                registrationAddress = Address(host, registrationPort)
                            )
                        )

                        val nodeClient = NodeClient(connectionManager)
                        val listener = NodeEventListener(connectionManager)

                        listener.start {
                            connectionManager.disconnect()
                            Cli.terminal.disconnectPrompt()
                        }

                        Cli.terminal.connectedPrompt(nodeClient.nodeName())
                    }.onFailure { ex ->
                        logger.info(
                            TranslationService.tr(
                                "cli",
                                "cli.connect.failed",
                                "message" to (ex.message ?: "unknown")
                            )
                        )
                    }
                }
            },
            "cli.command.impl.syntax.connect.description",
            hostArg, clusterPortArg, registrationPortArg, tokenArg
        )

        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info(TranslationService.tr("cli", "cli.connect.notConnected"))
                    return@syntax
                }

                connectionManager.disconnect()
                Cli.terminal.disconnectPrompt() //TODO remove certificates
            },
            "cli.command.impl.syntax.disconnect.description",
            KeywordArgument("disconnect")
        )
    }
}