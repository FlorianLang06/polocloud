package de.polocloud.cli.cluster

import de.polocloud.cli.connection.CliConnection
import de.polocloud.proto.ClusterServiceGrpcKt
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ProtoNodeData

class ClusterClient(
    private val connection: CliConnection
) {

    suspend fun listNodes(): List<ProtoNodeData> {
        val stub = ClusterServiceGrpcKt.ClusterServiceCoroutineStub(
            connection.channel()
        )

        val response = stub.listNodes(
            ListNodesRequest.newBuilder().build()
        )

        return response.nodesList
    }
}