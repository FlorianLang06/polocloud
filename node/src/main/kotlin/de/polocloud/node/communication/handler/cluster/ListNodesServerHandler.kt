package de.polocloud.node.communication.handler.cluster

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.cluster.node.toProto
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ListNodesResponse

class ListNodesServerHandler : GrpcServerHandler<ListNodesRequest, ListNodesResponse> {

    override suspend fun handle(
        request: ListNodesRequest,
        context: GrpcServerContext
    ): ListNodesResponse {
        val nodes = NodeRepository.findAll().map { it.toProto() }

        return ListNodesResponse.newBuilder()
            .addAllNodes(nodes)
            .build()
    }
}