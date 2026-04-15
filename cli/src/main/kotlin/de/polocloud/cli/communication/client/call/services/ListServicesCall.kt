package de.polocloud.cli.communication.client.call.services

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ProtoServiceProcessData
import de.polocloud.proto.ServiceManagerGrpcKt
import io.grpc.ManagedChannel

class ListServicesCall : GrpcClientCall<List<ProtoServiceProcessData>> {

    override suspend fun execute(channel: ManagedChannel): List<ProtoServiceProcessData> {
        val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(channel)
        val response = stub.listServices(ListServicesRequest.newBuilder().build())

        return response.serviceProcessList
    }
}