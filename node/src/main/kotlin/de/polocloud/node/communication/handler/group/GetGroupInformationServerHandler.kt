package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.group.GroupProtoMapper
import de.polocloud.node.group.GroupService
import de.polocloud.proto.GroupListRequest
import de.polocloud.proto.GroupListResponse

class GetGroupInformationServerHandler(val groupService: GroupService) : GrpcServerHandler<GroupListRequest, GroupListResponse> {

    override suspend fun handle(
        request: GroupListRequest,
        context: GrpcServerContext
    ): GroupListResponse {
        var groups = groupService.findAll().asSequence()

        if (request.hasNameFilter() && request.nameFilter.isNotBlank()) {
            groups = groups.filter { it.name.contains(request.nameFilter, ignoreCase = true) }
        }

        if (request.hasTypeFilter() && request.typeFilter.isNotBlank()) {
            groups = groups.filter { it.platform.equals(request.typeFilter, ignoreCase = true) }
        }

        return GroupListResponse.newBuilder()
            .addAllGroups(groups.map { GroupProtoMapper.toProto(it) }.toList())
            .build()
    }
}