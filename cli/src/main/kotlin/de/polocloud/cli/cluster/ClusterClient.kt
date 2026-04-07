package de.polocloud.cli.cluster

import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.proto.ClusterEvent
import de.polocloud.proto.ClusterEventRequest
import de.polocloud.proto.ClusterInfoRequest
import de.polocloud.proto.ClusterServiceGrpcKt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Client wrapper for cluster-related RPC calls.
 */
class ClusterClient(
    private val connectionManager: CliConnectionManager
) {

    fun nodeName(): String {
        check(connectionManager.isConnected) {
            "Not connected to cluster"
        }

        val stub = ClusterServiceGrpcKt
            .ClusterServiceCoroutineStub(connectionManager.channel())

        val response = runBlocking {
            stub.getClusterInfo(ClusterInfoRequest.newBuilder().build())
        }

        return response.nodeName //TODO get hole node information and build wrapper
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun listenForEvents(onShutdown: () -> Unit) {
        val stub = ClusterServiceGrpcKt
            .ClusterServiceCoroutineStub(connectionManager.channel())

        GlobalScope.launch {
            try {
                stub.listenForEvents(ClusterEventRequest.newBuilder().build())
                    .collect { event ->
                        when (event.type) {
                            ClusterEvent.Type.NODE_SHUTDOWN -> {
                                onShutdown()
                            }
                            else -> {}
                        }
                    }
            } catch (_: Exception) {

            }
        }
    }
}