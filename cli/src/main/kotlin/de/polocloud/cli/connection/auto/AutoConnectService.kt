package de.polocloud.cli.connection.auto

import de.polocloud.cli.Cli
import de.polocloud.cli.cluster.ClusterClient
import de.polocloud.cli.cluster.ClusterEventListener
import de.polocloud.cli.configuration.connection.ConnectionHistory
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.connection.lifecycle.ConnectionLifecycle
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.i18n.trWarn
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class AutoConnectService(
    private val connectionManager: CliConnectionManager,
    private val history: ConnectionHistory,
) : ConnectionLifecycle {

    private val logger = LoggerFactory.getLogger(CliConnectionManager::class.java)
    private var eventListener: ClusterEventListener? = null

    override fun start() {
        val last = history.latest() ?: return
        if (!connectionManager.isRegistered()) return

        logger.trInfo("cli", "cli.connect.auto.connecting", "host" to last.clusterAddress)

        runBlocking {
            runCatching {
                connectionManager.connect(
                    last.clusterAddress,
                    last.registrationAddress
                )
            }.onSuccess {
                attachListener()
                Cli.terminal.connectedPrompt(ClusterClient(connectionManager).nodeName())
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
        eventListener = ClusterEventListener(connectionManager).also { listener ->
            listener.start {
                connectionManager.disconnect()
                Cli.terminal.disconnectPrompt()
            }
        }
    }
}