package de.polocloud.node.communication.impl.node

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.node.event.ClusterEventService
import de.polocloud.proto.EventContext
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse
import de.polocloud.proto.NodeServiceGrpcKt
import de.polocloud.proto.RelayEventRequest
import de.polocloud.proto.RelayEventResponse
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NodeServiceImpl(
    private val executor: GrpcServerExecutor,
) : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {

    private val listeners = mutableSetOf<SendChannel<NodeEvent>>()

    override suspend fun getNodeInformation(request: NodeInformationRequest): NodeInformationResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override fun listenForEvents(request: NodeEventRequest): Flow<NodeEvent> = callbackFlow {
        listeners += channel

        awaitClose { listeners -= channel }
    }

    /**
     * Receives an event relayed from a peer node and re-broadcasts it to this node's
     * local subscribers only (via [ClusterEventService.broadcast], which does not relay
     * again) — so a peer's event reaches local bridges/SDKs without creating a loop.
     */
    override suspend fun relayEvent(request: RelayEventRequest): RelayEventResponse {
        ClusterEventService.broadcast(
            EventContext.newBuilder()
                .setEventName(request.eventName)
                .setEventData(request.eventData)
                .build()
        )
        return RelayEventResponse.newBuilder().setSuccess(true).build()
    }

    fun broadcastShutdown() {
        val event = NodeEvent.newBuilder()
            .setType(NodeEvent.Type.NODE_SHUTDOWN)
            .build()

        listeners.forEach { it.trySend(event) }
    }
}