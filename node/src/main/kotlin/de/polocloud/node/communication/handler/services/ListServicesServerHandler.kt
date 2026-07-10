package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.ServiceProcessProtoMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.NodePeerServiceQuery
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoServiceProcessData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Handles the CLI `ListServices` request.
 *
 * Returns the cluster-wide view: this node's own services plus the local services of
 * every other online node, queried in parallel. Peers are asked with `local_only = true`
 * so they never fan out again (recursion guard), and a slow or unreachable peer is simply
 * skipped rather than failing or hanging the whole listing.
 *
 * For a single-node setup there are no peers, so this behaves exactly like a local listing.
 *
 * @param peers supplies the nodes to aggregate over — injectable for testing.
 * @param peerQuery fetches a peer's local services — injectable for testing.
 */
class ListServicesServerHandler(
    private val serviceProvider: ServiceProvider,
    private val peers: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val peerQuery: PeerServiceQuery = NodePeerServiceQuery(),
) : GrpcServerHandler<ListServicesRequest, ListServicesResponse> {

    private val logger = LoggerFactory.getLogger(ListServicesServerHandler::class.java)

    override suspend fun handle(
        request: ListServicesRequest,
        context: GrpcServerContext
    ): ListServicesResponse {
        val local = localServices(request.planName)

        // A peer-scoped request (or genuinely no peers) returns only this node's services.
        val remote = if (request.localOnly) emptyList() else remoteServices(request.planName)

        return ListServicesResponse.newBuilder()
            .addAllServiceProcess(local + remote)
            .build()
    }

    private fun localServices(planName: String): List<ProtoServiceProcessData> {
        // Snapshot first: the list is mutated by the queue and prune threads.
        var services = serviceProvider.localServices.toList().asSequence()
        if (planName.isNotBlank()) {
            services = services.filter { it.groupName.equals(planName, ignoreCase = true) }
        }
        return services.map { ServiceProcessProtoMapper.toProto(it, serviceProvider.nodeId) }.toList()
    }

    private suspend fun remoteServices(planName: String): List<ProtoServiceProcessData> {
        val others = peers().filter { it.id.toString() != serviceProvider.nodeId }
        if (others.isEmpty()) return emptyList()

        return coroutineScope {
            others.map { node ->
                async {
                    // Isolate each peer: one that is down or slow must not break the listing.
                    runCatching { peerQuery.localServicesOf(node, planName) }
                        .onFailure { logger.warn("Failed to query services from node {}: {}", node.name(), it.message) }
                        .getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
    }
}
