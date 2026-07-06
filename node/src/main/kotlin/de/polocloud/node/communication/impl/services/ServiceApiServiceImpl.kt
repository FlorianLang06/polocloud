package de.polocloud.node.communication.impl.services

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.ServiceListResponse

/**
 * gRPC entry point of the API-facing `ServiceApiService`, hosted on the
 * [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint].
 *
 * Delegates to the shared [GrpcServerExecutor] so it runs through the same
 * middleware pipeline as every other request. Mirrors
 * [de.polocloud.node.communication.impl.group.GroupApiServiceImpl].
 */
class ServiceApiServiceImpl(
    private val executor: GrpcServerExecutor,
) : ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineImplBase() {

    override suspend fun findServices(request: ServiceListRequest): ServiceListResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }
}