package de.polocloud.cli.cluster

import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.proto.ClusterEvent
import de.polocloud.proto.ClusterEventRequest
import de.polocloud.proto.ClusterServiceGrpcKt
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class ClusterEventListener(
    private val connectionManager: CliConnectionManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start(onShutdown: () -> Unit) {
        if (job != null) return

        job = scope.launch {
            val stub = ClusterServiceGrpcKt
                .ClusterServiceCoroutineStub(connectionManager.channel())

            try {
                stub.listenForEvents(ClusterEventRequest.newBuilder().build())
                    .collect { event ->
                        handleEvent(event, onShutdown)
                    }
            } catch (ex: Exception) {
                logger.debug("Event stream closed: ${ex.message}")

                if (connectionManager.isConnected) {
                    logger.info("Connection lost (event stream closed)")
                    onShutdown()
                }
            }
        }
    }

    private fun handleEvent(event: ClusterEvent, onShutdown: () -> Unit) {
        when (event.type) {
            ClusterEvent.Type.NODE_SHUTDOWN -> {
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