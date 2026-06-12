package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse

class ListServicesServerHandler : GrpcServerHandler<ListServicesRequest, ListServicesResponse> {

    override suspend fun handle(
        request: ListServicesRequest,
        context: GrpcServerContext
    ): ListServicesResponse {
      TODO()

        return ListServicesResponse.newBuilder()

            .build()
    }
}