package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.process.ServiceProcessRepository
import de.polocloud.node.services.process.toProto
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse

class ListServicesServerHandler : GrpcServerHandler<ListServicesRequest, ListServicesResponse> {

    override suspend fun handle(
        request: ListServicesRequest,
        context: GrpcServerContext
    ): ListServicesResponse {
        val services = ServiceProcessRepository.findAll().map { it.toProto() }

        return ListServicesResponse.newBuilder()
            .addAllServiceProcess(services)
            .build()
    }
}