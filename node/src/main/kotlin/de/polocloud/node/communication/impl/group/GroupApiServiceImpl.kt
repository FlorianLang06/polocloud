package de.polocloud.node.communication.impl.group

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.CreateGroupRequest
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.DeleteGroupResponse
import de.polocloud.proto.GroupApiServiceGrpcKt
import de.polocloud.proto.GroupData
import de.polocloud.proto.GroupListRequest
import de.polocloud.proto.GroupListResponse
import de.polocloud.proto.UpdateGroupRequest

class GroupApiServiceImpl(
    private val executor: GrpcServerExecutor,
) : GroupApiServiceGrpcKt.GroupApiServiceCoroutineImplBase() {

    override suspend fun findGroups(request: GroupListRequest): GroupListResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun createGroup(request: CreateGroupRequest): GroupData {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun updateGroup(request: UpdateGroupRequest): GroupData {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun deleteGroup(request: DeleteGroupRequest): DeleteGroupResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }
}
