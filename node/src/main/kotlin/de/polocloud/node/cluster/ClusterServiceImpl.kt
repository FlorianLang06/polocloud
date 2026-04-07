package de.polocloud.node.cluster

import de.polocloud.node.nodes.LocalNodeContainer
import de.polocloud.proto.ClusterEvent
import de.polocloud.proto.ClusterEventRequest
import de.polocloud.proto.ClusterInfoRequest
import de.polocloud.proto.ClusterInfoResponse
import de.polocloud.proto.ClusterServiceGrpcKt
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Provides cluster-related information to CLI clients.
 */
class ClusterServiceImpl(
    private val localNodeContainerProvider: () -> LocalNodeContainer
) : ClusterServiceGrpcKt.ClusterServiceCoroutineImplBase() {

    private val listeners = mutableSetOf<SendChannel<ClusterEvent>>()

    override suspend fun getClusterInfo(request: ClusterInfoRequest): ClusterInfoResponse {
        val node = localNodeContainerProvider().data

        return ClusterInfoResponse.newBuilder()
            .setNodeName(node.name())
            .build()
    }

    override fun listenForEvents(
        request: ClusterEventRequest
    ): Flow<ClusterEvent> = callbackFlow {

        listeners += channel

        awaitClose {
            listeners -= channel
        }
    }

    fun broadcastShutdown() {
        val event = ClusterEvent.newBuilder()
            .setType(ClusterEvent.Type.NODE_SHUTDOWN)
            .build()

        listeners.forEach {
            it.trySend(event)
        }
    }
}