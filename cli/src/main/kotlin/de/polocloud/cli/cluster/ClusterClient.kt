package de.polocloud.cli.cluster

import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.proto.ClusterInfoRequest
import de.polocloud.proto.ClusterServiceGrpcKt
import kotlinx.coroutines.runBlocking

/**
 * Client wrapper for cluster-related RPC calls.
 */
class ClusterClient(
    private val connectionManager: CliConnectionManager
) {

    fun nodeName(): String {
        check(connectionManager.isConnected) {
            "Not connected to cluster"
        }

        val stub = ClusterServiceGrpcKt
            .ClusterServiceCoroutineStub(connectionManager.channel())

        val response = runBlocking {
            stub.getClusterInfo(ClusterInfoRequest.newBuilder().build())
        }

        return response.nodeName //TODO get hole node information and build wrapper
    }
}