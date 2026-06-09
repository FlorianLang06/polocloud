package de.polocloud.node.group

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import de.polocloud.node.cluster.node.NodeData

class GroupRepository {

    private val groupDatabaseKey = DatabaseKey(Group::class)

    fun find(name: String) = DatabaseAccess.executor().findById(groupDatabaseKey,  name)

    fun save(group: Group) = DatabaseAccess.executor().save(groupDatabaseKey, group)

    fun findAll() = DatabaseAccess.executor().findAll(groupDatabaseKey)

    fun count() = DatabaseAccess.executor().count(groupDatabaseKey)

}