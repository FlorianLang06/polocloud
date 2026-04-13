package de.polocloud.cli.communication.client.call.cluster

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.proto.ClusterServiceGrpcKt
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ProtoNodeData
import io.grpc.ManagedChannel

class ListNodesCall : GrpcClientCall<List<ProtoNodeData>> {

    override suspend fun execute(channel: ManagedChannel): List<ProtoNodeData> {
        val stub = ClusterServiceGrpcKt.ClusterServiceCoroutineStub(channel)
        val response = stub.listNodes(ListNodesRequest.newBuilder().build())

        return response.nodesList
    }
}