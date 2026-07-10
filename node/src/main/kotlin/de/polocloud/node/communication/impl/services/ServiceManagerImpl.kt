package de.polocloud.node.communication.impl.services

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse
import de.polocloud.proto.ServiceManagerGrpcKt

class ServiceManagerImpl(
    private val executor: GrpcServerExecutor,
) : ServiceManagerGrpcKt.ServiceManagerCoroutineImplBase() {

    override suspend fun listServices(request: ListServicesRequest): ListServicesResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }
}
