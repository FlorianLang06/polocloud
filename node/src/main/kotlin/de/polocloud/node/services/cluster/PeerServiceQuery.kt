package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ProtoServiceProcessData
import de.polocloud.proto.ServiceManagerGrpcKt
import kotlinx.coroutines.withTimeout

/**
 * Fetches the *local* services of a single peer node — the building block of the
 * cluster-wide service view assembled in
 * [de.polocloud.node.communication.handler.services.ListServicesServerHandler].
 *
 * Kept behind an interface so the aggregation logic can be unit-tested without opening
 * a real gRPC channel.
 */
fun interface PeerServiceQuery {

    /** Returns the services running locally on [node]. Filtered by [planName] if non-blank. */
    suspend fun localServicesOf(node: NodeData, planName: String): List<ProtoServiceProcessData>
}

/**
 * Real [PeerServiceQuery]: opens a short-lived mTLS channel to the peer's node endpoint
 * ([NodeData.hostname]:[NodeData.port], which hosts `ServiceManager`) and asks it for its
 * local services (`local_only = true`, so the peer does not fan out again).
 */
class NodePeerServiceQuery(
    private val timeoutMillis: Long = 3_000,
) : PeerServiceQuery {

    override suspend fun localServicesOf(node: NodeData, planName: String): List<ProtoServiceProcessData> {
        val client = NodeGrpcClient()
        return try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = ListServicesRequest.newBuilder()
                .setPlanName(planName)
                .setLocalOnly(true)
                .build()
            withTimeout(timeoutMillis) { stub.listServices(request).serviceProcessList }
        } finally {
            client.disconnect()
        }
    }
}
