package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProcessProtoMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse

/**
 * Handles the CLI `ListServices` request by returning the node's currently running
 * services as [de.polocloud.proto.ProtoServiceProcessData].
 */
class ListServicesServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<ListServicesRequest, ListServicesResponse> {

    override suspend fun handle(
        request: ListServicesRequest,
        context: GrpcServerContext
    ): ListServicesResponse {
        // Snapshot first: the list is mutated by the queue and prune threads.
        val snapshot = serviceProvider.localServices.toList()
        var services = snapshot.asSequence()

        if (request.planName.isNotBlank()) {
            services = services.filter { it.groupName.equals(request.planName, ignoreCase = true) }
        }

        return ListServicesResponse.newBuilder()
            .addAllServiceProcess(services.map(ServiceProcessProtoMapper::toProto).toList())
            .build()
    }
}
