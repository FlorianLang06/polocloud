package de.polocloud.node.cluster

import de.polocloud.node.nodes.LocalNodeContainer
import de.polocloud.proto.ClusterInfoRequest
import de.polocloud.proto.ClusterInfoResponse
import de.polocloud.proto.ClusterServiceGrpcKt
import org.slf4j.LoggerFactory

/**
 * Provides cluster-related information to CLI clients.
 */
class ClusterServiceImpl(
    private val localNodeContainerProvider: () -> LocalNodeContainer
) : ClusterServiceGrpcKt.ClusterServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)

    override suspend fun getClusterInfo(request: ClusterInfoRequest): ClusterInfoResponse {
        val node = localNodeContainerProvider().data

        return ClusterInfoResponse.newBuilder()
            .setNodeName(node.name())
            .build()
    }
}