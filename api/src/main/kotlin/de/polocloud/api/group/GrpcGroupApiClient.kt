package de.polocloud.api.group

import de.polocloud.proto.CreateGroupRequest
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.GroupApiServiceGrpcKt
import de.polocloud.proto.GroupData
import de.polocloud.proto.GroupListRequest
import de.polocloud.proto.UpdateGroupRequest
import io.grpc.ManagedChannel

/**
 * gRPC-backed [GroupApiClient] that talks to the node's `GroupApiService`.
 *
 * The channel is obtained lazily through [channelProvider] so the connection is
 * only opened when a call is actually made.
 */
class GrpcGroupApiClient(
    private val channelProvider: () -> ManagedChannel,
) : GroupApiClient {

    private fun stub() = GroupApiServiceGrpcKt.GroupApiServiceCoroutineStub(channelProvider())

    override suspend fun findGroups(nameFilter: String?, typeFilter: String?): List<GroupData> {
        val request = GroupListRequest.newBuilder().apply {
            nameFilter?.let { setNameFilter(it) }
            typeFilter?.let { setTypeFilter(it) }
        }.build()

        return stub().findGroups(request).groupsList
    }

    override suspend fun createGroup(data: GroupData): GroupData {
        val request = CreateGroupRequest.newBuilder().setGroup(data).build()
        return stub().createGroup(request)
    }

    override suspend fun updateGroup(data: GroupData): GroupData {
        val request = UpdateGroupRequest.newBuilder().setGroup(data).build()
        return stub().updateGroup(request)
    }

    override suspend fun deleteGroup(name: String): Boolean {
        val request = DeleteGroupRequest.newBuilder().setName(name).build()
        return stub().deleteGroup(request).success
    }
}
