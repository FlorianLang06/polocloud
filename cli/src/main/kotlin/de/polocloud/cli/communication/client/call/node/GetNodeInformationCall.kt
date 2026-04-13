package de.polocloud.cli.communication.client.call.node

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeServiceGrpcKt
import io.grpc.ManagedChannel

class GetNodeInformationCall : GrpcClientCall<String> {

    override suspend fun execute(channel: ManagedChannel): String {
        val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channel)
        val response = stub.getNodeInformation(NodeInformationRequest.newBuilder().build())

        return response.nodeName
    }
}