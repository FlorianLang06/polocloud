package de.polocloud.node.communication.handler.cluster

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.handler.GrpcHandler
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.cluster.node.toProto
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ListNodesResponse

class ListNodesHandler : GrpcHandler<ListNodesRequest, ListNodesResponse> {

    override suspend fun handle(
        request: ListNodesRequest,
        context: GrpcContext
    ): ListNodesResponse {
        val nodes = NodeRepository.findAll().map { it.toProto() }

        return ListNodesResponse.newBuilder()
            .addAllNodes(nodes)
            .build()
    }
}