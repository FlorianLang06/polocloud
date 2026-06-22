package de.polocloud.api.group

import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

/**
 * Public, blocking entry point to the cluster's group API.
 *
 * Backed by a [GroupApiClient] (gRPC in production). Obtain the shared instance
 * via [de.polocloud.api.Polocloud.groupService].
 */
class GroupService internal constructor(
    private val client: GroupApiClient,
) {

    fun findAll(): List<Group> =
        runBlocking { client.findGroups(null, null) }.map(GroupMapper::toApi)

    fun find(name: String): Group? =
        runBlocking { client.findGroups(name, null) }
            .map(GroupMapper::toApi)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun find(type: GroupFilterType): List<Group> =
        findAll().filter { type.matches(it.platform) }

    fun count(): Int = findAll().size

    fun delete(name: String) {
        runBlocking { client.deleteGroup(name) }
    }

    fun delete(group: Group) = delete(group.name)

    fun edit(editor: Consumer<GroupBuilder>) {
        val builder = GroupBuilder { group ->
            GroupMapper.toApi(runBlocking { client.updateGroup(GroupMapper.toProto(group)) })
        }
        editor.accept(builder)
        builder.submit()
    }

    fun create(name: String): GroupBuilder =
        GroupBuilder { group ->
            GroupMapper.toApi(runBlocking { client.createGroup(GroupMapper.toProto(group)) })
        }.name(name)
}
