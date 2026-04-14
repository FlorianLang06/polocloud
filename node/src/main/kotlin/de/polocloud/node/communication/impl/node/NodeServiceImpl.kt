package de.polocloud.node.communication.impl.node

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse
import de.polocloud.proto.NodeServiceGrpcKt
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

    fun broadcastShutdown() {
        val event = NodeEvent.newBuilder()
            .setType(NodeEvent.Type.NODE_SHUTDOWN)
            .build()

        listeners.forEach { it.trySend(event) }
    }
}