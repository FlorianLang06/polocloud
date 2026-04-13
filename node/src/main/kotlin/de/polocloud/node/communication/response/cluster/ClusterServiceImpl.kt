package de.polocloud.node.communication.response.cluster

import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.cluster.node.toProto
import de.polocloud.proto.ClusterServiceGrpcKt
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ListNodesResponse

class ClusterServiceImpl: ClusterServiceGrpcKt.ClusterServiceCoroutineImplBase() {

    override suspend fun listNodes(
        request: ListNodesRequest
    ): ListNodesResponse {
        val nodes = NodeRepository.findAll().map { it.toProto() }
        val response = ListNodesResponse.newBuilder().addAllNodes(nodes)

        return response.build()
    }
}