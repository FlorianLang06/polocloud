package de.polocloud.node.communication.impl.cluster

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.ClusterServiceGrpcKt
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ListNodesResponse

class ClusterServiceImpl(
    private val executor: GrpcServerExecutor
) : ClusterServiceGrpcKt.ClusterServiceCoroutineImplBase() {

    override suspend fun listNodes(request: ListNodesRequest): ListNodesResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }
}