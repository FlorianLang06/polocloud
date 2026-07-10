package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.ServiceProtoMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.NodePeerServiceDataQuery
import de.polocloud.node.services.cluster.PeerServiceDataQuery
import de.polocloud.proto.NodeState
import de.polocloud.proto.ServiceData
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.ServiceListResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Handles the API/SDK `FindServices` request (consumed by the plugin/service SDK and the
 * proxy bridge).
 *
 * Returns the cluster-wide view — this node's services plus every other online node's
 * local services, carrying each service's `host` so a proxy can connect to services on
 * other nodes. Peers are asked with `local_only = true` (recursion guard) and a slow or
 * unreachable peer is skipped rather than failing the listing. Single-node setups have no
 * peers and behave exactly like a local listing.
 *
 * @param peers supplies the nodes to aggregate over — injectable for testing.
 * @param peerQuery fetches a peer's local services — injectable for testing.
 */
class FindServicesServerHandler(
    private val serviceProvider: ServiceProvider,
    private val peers: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val peerQuery: PeerServiceDataQuery = NodePeerServiceDataQuery(),
) : GrpcServerHandler<ServiceListRequest, ServiceListResponse> {

    private val logger = LoggerFactory.getLogger(FindServicesServerHandler::class.java)

    override suspend fun handle(
        request: ServiceListRequest,
        context: GrpcServerContext,
    ): ServiceListResponse {
        val groupFilter = if (request.hasGroupFilter() && request.groupFilter.isNotBlank()) request.groupFilter else null
        val stateFilter = if (request.hasStateFilter() && request.stateFilter.isNotBlank()) request.stateFilter else null

        val local = localServices(groupFilter, stateFilter)
        val remote = if (request.localOnly) emptyList() else remoteServices(groupFilter, stateFilter)

        return ServiceListResponse.newBuilder()
            .addAllServices(local + remote)
            .build()
    }

    private fun localServices(groupFilter: String?, stateFilter: String?): List<ServiceData> {
        // Snapshot first: the list is mutated by the queue and prune threads.
        var services = serviceProvider.localServices.toList().asSequence()
        if (groupFilter != null) services = services.filter { it.groupName.equals(groupFilter, ignoreCase = true) }
        if (stateFilter != null) services = services.filter { it.state.name.equals(stateFilter, ignoreCase = true) }
        return services.map(ServiceProtoMapper::toProto).toList()
    }

    private suspend fun remoteServices(groupFilter: String?, stateFilter: String?): List<ServiceData> {
        val others = peers().filter { it.id.toString() != serviceProvider.nodeId }
        if (others.isEmpty()) return emptyList()

        return coroutineScope {
            others.map { node ->
                async {
                    runCatching { peerQuery.localServicesOf(node, groupFilter, stateFilter) }
                        .onFailure { logger.warn("Failed to query services from node {}: {}", node.name(), it.message) }
                        .getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
    }
}
