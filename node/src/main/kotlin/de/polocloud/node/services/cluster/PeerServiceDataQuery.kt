package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceData
import de.polocloud.proto.ServiceListRequest
import kotlinx.coroutines.withTimeout

/**
 * Fetches the *local* [ServiceData] of a single peer node — the SDK/bridge-facing
 * counterpart of [PeerServiceQuery]. Unlike the CLI shape, [ServiceData] carries the
 * service `host`, so a proxy can actually connect to services on other nodes.
 *
 * Kept behind an interface so the aggregation logic can be unit-tested without a real
 * gRPC channel.
 */
fun interface PeerServiceDataQuery {

    /** Returns the [ServiceData] running locally on [node], filtered by [groupFilter]/[stateFilter]. */
    suspend fun localServicesOf(node: NodeData, groupFilter: String?, stateFilter: String?): List<ServiceData>
}

/**
 * Real [PeerServiceDataQuery]: opens a short-lived mTLS channel to the peer's node
 * endpoint ([NodeData.hostname]:[NodeData.port], which also hosts `ServiceApiService`)
 * and asks for its local services (`local_only = true`).
 */
class NodePeerServiceDataQuery(
    private val timeoutMillis: Long = 3_000,
) : PeerServiceDataQuery {

    override suspend fun localServicesOf(
        node: NodeData,
        groupFilter: String?,
        stateFilter: String?,
    ): List<ServiceData> {
        val client = NodeGrpcClient()
        return try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(client.channel())
            val request = ServiceListRequest.newBuilder().apply {
                localOnly = true
                groupFilter?.let { setGroupFilter(it) }
                stateFilter?.let { setStateFilter(it) }
            }.build()
            withTimeout(timeoutMillis) { stub.findServices(request).servicesList }
        } finally {
            client.disconnect()
        }
    }
}
