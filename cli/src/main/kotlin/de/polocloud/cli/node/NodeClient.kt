package de.polocloud.cli.node

import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeServiceGrpcKt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Client wrapper for cluster-related RPC calls.
 */
class NodeClient(
    private val connectionManager: CliConnectionManager
) {

    fun nodeName(): String {
        check(connectionManager.isConnected) {
            "Not connected to cluster"
        }

        val stub = NodeServiceGrpcKt
            .NodeServiceCoroutineStub(connectionManager.channel())

        val response = runBlocking {
            stub.getNodeInformation(NodeInformationRequest.newBuilder().build())
        }

        return response.nodeName //TODO get hole node information and build wrapper
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun listenForEvents(onShutdown: () -> Unit) {
        val stub = NodeServiceGrpcKt
            .NodeServiceCoroutineStub(connectionManager.channel())

        GlobalScope.launch {
            try {
                stub.listenForEvents(NodeEventRequest.newBuilder().build())
                    .collect { event ->
                        when (event.type) {
                            NodeEvent.Type.NODE_SHUTDOWN -> {
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