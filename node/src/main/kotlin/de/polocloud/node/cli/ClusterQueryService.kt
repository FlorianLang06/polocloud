package de.polocloud.node.cli

import de.polocloud.node.repositories.NodeRepository
import de.polocloud.proto.ClusterQueryServiceGrpcKt
import de.polocloud.proto.Empty
import de.polocloud.proto.NodeInfo
import de.polocloud.proto.NodeListResponse
import de.polocloud.proto.ShutdownNodeRequest
import de.polocloud.proto.ShutdownNodeResponse
import java.util.UUID

class ClusterQueryService(
    private val nodeRepository: NodeRepository,
) : ClusterQueryServiceGrpcKt.ClusterQueryServiceCoroutineImplBase() {

    override suspend fun listNodes(request: Empty): NodeListResponse {
        val nodes = nodeRepository.findAll().map { node ->
            NodeInfo.newBuilder()
                .setId(node.id.toString())
                .setHostname(node.hostname)
                .setPort(node.port)
                .setState(node.state.name)
                .setHead(node.head)
                .setVersion(node.version)
                .build()
        }

        return NodeListResponse.newBuilder()
            .addAllNodes(nodes)
            .build()
    }

    override suspend fun shutdownNode(request: ShutdownNodeRequest): ShutdownNodeResponse {
        val id = runCatching { UUID.fromString(request.nodeId) }.getOrNull()
            ?: return ShutdownNodeResponse.newBuilder()
                .setAccepted(false)
                .setMessage("Invalid UUID")
                .build()

        val node = nodeRepository.find(id)
            ?: return ShutdownNodeResponse.newBuilder()
                .setAccepted(false)
                .setMessage("Node not found")
                .build()

        // TODO: gRPC-Call an den Ziel-Node senden
        return ShutdownNodeResponse.newBuilder()
            .setAccepted(true)
            .build()
    }
}