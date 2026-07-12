package de.polocloud.node.services

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import de.polocloud.database.filtering.Eq
import java.util.UUID

object ServiceRepository {

    private val serviceDatabaseKey = DatabaseKey(Service::class)

    fun save(service: Service) = DatabaseAccess.executor().save(serviceDatabaseKey, service)

    fun delete(service: Service) = DatabaseAccess.executor().delete(serviceDatabaseKey, service)

    fun findAll() = DatabaseAccess.executor().findAll(serviceDatabaseKey)

    fun findAllForNode(nodeId: String) = DatabaseAccess.executor().find(serviceDatabaseKey, Eq("nodeId", nodeId))

    fun findByGroup(groupName: String) = DatabaseAccess.executor().find(serviceDatabaseKey, Eq("groupName", groupName))
    
    fun findById(id: UUID) = DatabaseAccess.executor().findById(serviceDatabaseKey, id)

    fun count() = DatabaseAccess.executor().count(serviceDatabaseKey)

}