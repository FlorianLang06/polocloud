package de.polocloud.cli.communication.connection

import de.polocloud.i18n.api.trInfo
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeServiceGrpcKt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ShutdownEventListener(
    private val connectionManager: CliConnectionManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start(onShutdown: () -> Unit) {
        if (job != null) return

        job = scope.launch {
            val stub = NodeServiceGrpcKt
                .NodeServiceCoroutineStub(connectionManager.channel())

            try {
                stub.listenForEvents(NodeEventRequest.newBuilder().build())
                    .collect { event ->
                        handleEvent(event, onShutdown)
                    }
            } catch (ex: Exception) {
                logger.debug("Event stream closed: ${ex.message}")
                ex.printStackTrace()

                if (connectionManager.isConnected) {
                    logger.trInfo("cli", "cli.connect.connection.lost")
                    onShutdown()
                }
            }
        }
    }

    private fun handleEvent(event: NodeEvent, onShutdown: () -> Unit) {
        when (event.type) {
            NodeEvent.Type.NODE_SHUTDOWN -> {
                onShutdown()
            }
            else -> {}
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}