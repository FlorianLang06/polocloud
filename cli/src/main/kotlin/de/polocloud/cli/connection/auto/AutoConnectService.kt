package de.polocloud.cli.connection.auto

import de.polocloud.cli.Cli
import de.polocloud.cli.communication.CliSession
import de.polocloud.cli.configuration.connection.ConnectionHistory
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.connection.lifecycle.ConnectionLifecycle
import de.polocloud.cli.communication.client.impl.node.NodeClientImpl
import de.polocloud.cli.node.NodeEventListener
import de.polocloud.i18n.api.trInfo
import de.polocloud.i18n.api.trWarn
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class AutoConnectService(
    private val connectionManager: CliConnectionManager,
    private val history: ConnectionHistory,
) : ConnectionLifecycle {

    private val logger = LoggerFactory.getLogger(CliConnectionManager::class.java)
    private var eventListener: NodeEventListener? = null

    override fun start() {
        val last = history.latest() ?: return
        if (!connectionManager.isRegistered()) return

        logger.trInfo("cli", "cli.connect.auto.connecting", "host" to last.clusterAddress)

        var nodeName = "cli" //this is the default in case the connection not works
        runBlocking {
            runCatching {
                connectionManager.connect(
                    last.clusterAddress,
                    last.registrationAddress
                )

                Cli.session = CliSession(connectionManager)
                nodeName = Cli.session.nodeClient.nodeName()
            }.onSuccess {
                attachListener()
                Cli.terminal.connectedPrompt(nodeName)
            }.onFailure {
                logger.trWarn("cli", "cli.connect.auto.failed", "message" to it.message)
            }
        }
    }

    override fun stop() {
        eventListener?.stop()
        eventListener = null
        if (connectionManager.isConnected) {
            connectionManager.disconnect()
        }
    }

    fun attachListener() {
        eventListener = NodeEventListener(connectionManager).also { listener ->
            listener.start {
                connectionManager.disconnect()
                Cli.terminal.disconnectPrompt()
            }
        }
    }
}