package de.polocloud.node.group

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey

object GroupRepository {

    private val groupDatabaseKey = DatabaseKey(Group::class)

    fun find(name: String) = DatabaseAccess.executor().findById(groupDatabaseKey,  name)

    fun save(group: Group) = DatabaseAccess.executor().save(groupDatabaseKey, group)

    fun delete(group: Group) = DatabaseAccess.executor().delete(groupDatabaseKey, group)

    fun findAll() = DatabaseAccess.executor().findAll(groupDatabaseKey)

    fun count() = DatabaseAccess.executor().count(groupDatabaseKey)

    fun exists(name: String) = DatabaseAccess.executor().findById(groupDatabaseKey,  name) != null


}