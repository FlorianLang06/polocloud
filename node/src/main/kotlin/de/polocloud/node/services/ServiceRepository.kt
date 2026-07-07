package de.polocloud.node.services

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey

object ServiceRepository {

    private val serviceDatabaseKey = DatabaseKey(Service::class)

    fun find(name: String) = DatabaseAccess.executor().findById(serviceDatabaseKey,  name)

    fun save(service: Service) = DatabaseAccess.executor().save(serviceDatabaseKey, service)

    fun delete(service: Service) = DatabaseAccess.executor().delete(serviceDatabaseKey, service)

    fun findAll() = DatabaseAccess.executor().findAll(serviceDatabaseKey)

    fun count() = DatabaseAccess.executor().count(serviceDatabaseKey)

    fun exists(name: String) = DatabaseAccess.executor().findById(serviceDatabaseKey,  name) != null

}