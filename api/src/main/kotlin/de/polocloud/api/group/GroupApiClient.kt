package de.polocloud.api.group

import de.polocloud.proto.GroupData

/**
 * Transport-agnostic gateway to the node's group API.
 *
 * Implemented by [GrpcGroupApiClient] for real gRPC traffic; abstracted as an
 * interface so the [GroupService] can be unit-tested without a live node.
 */
interface GroupApiClient {

    suspend fun findGroups(nameFilter: String?, typeFilter: String?): List<GroupData>

    suspend fun createGroup(data: GroupData): GroupData

    suspend fun updateGroup(data: GroupData): GroupData

    suspend fun deleteGroup(name: String): Boolean
}
