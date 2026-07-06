package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProtoMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.ServiceListResponse
import org.slf4j.LoggerFactory

/**
 * Handles API `FindServices` requests by returning the node's currently known
 * services, optionally filtered by group and/or state.
 *
 * Mirrors [de.polocloud.node.communication.handler.group.GetGroupInformationServerHandler].
 */
class FindServicesServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<ServiceListRequest, ServiceListResponse> {

    override suspend fun handle(
        request: ServiceListRequest,
        context: GrpcServerContext,
    ): ServiceListResponse {
        // Snapshot first: the list is mutated by the queue and prune threads.
        val snapshot = serviceProvider.localServices.toList()
        var services = snapshot.asSequence()

        if (request.hasGroupFilter() && request.groupFilter.isNotBlank()) {
            services = services.filter { it.group.equals(request.groupFilter, ignoreCase = true) }
        }

        if (request.hasStateFilter() && request.stateFilter.isNotBlank()) {
            services = services.filter { it.state.name.equals(request.stateFilter, ignoreCase = true) }
        }

        return ServiceListResponse.newBuilder()
            .addAllServices(services.map { ServiceProtoMapper.toProto(it) }.toList())
            .build()
    }
}