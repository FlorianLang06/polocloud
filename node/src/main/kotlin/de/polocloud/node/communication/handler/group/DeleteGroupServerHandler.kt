package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.group.GroupService
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.DeleteGroupResponse

class DeleteGroupServerHandler(private val groupService: GroupService) : GrpcServerHandler<DeleteGroupRequest, DeleteGroupResponse> {

    override suspend fun handle(
        request: DeleteGroupRequest,
        context: GrpcServerContext
    ): DeleteGroupResponse {
        val group = groupService.find(request.name)
            ?: return DeleteGroupResponse.newBuilder().setSuccess(false).build()

        groupService.delete(group)

        return DeleteGroupResponse.newBuilder().setSuccess(true).build()
    }
}
