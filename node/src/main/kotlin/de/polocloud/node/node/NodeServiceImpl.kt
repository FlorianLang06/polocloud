package de.polocloud.node.node

import de.polocloud.node.nodes.LocalNodeContainer
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse
import de.polocloud.proto.NodeServiceGrpcKt
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Provides cluster-related information to CLI clients.
 */
class NodeServiceImpl(
    private val localNodeContainerProvider: () -> LocalNodeContainer
) : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {

    private val listeners = mutableSetOf<SendChannel<NodeEvent>>()

    override suspend fun getNodeInformation(request: NodeInformationRequest): NodeInformationResponse {
        val node = localNodeContainerProvider().data

        return NodeInformationResponse.newBuilder()
            .setNodeName(node.name())
            .build()
    }

    override fun listenForEvents(
        request: NodeEventRequest
    ): Flow<NodeEvent> = callbackFlow {

        listeners += channel

        awaitClose {
            listeners -= channel
        }
    }

    fun broadcastShutdown() {
        val event = NodeEvent.newBuilder()
            .setType(NodeEvent.Type.NODE_SHUTDOWN)
            .build()

        listeners.forEach {
            it.trySend(event)
        }
    }
}