package de.polocloud.node.services.process

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import java.util.UUID

object ServiceProcessRepository {

    private val databaseKey = DatabaseKey(ServiceProcess::class)

    fun update(process: ServiceProcess) {
        DatabaseAccess.executor().save(databaseKey, process)
    }

    fun find(id: UUID) = DatabaseAccess.executor().findById(databaseKey, id)

    fun findAll(): List<ServiceProcess> = DatabaseAccess.executor().findAll(databaseKey)

    fun delete(process: ServiceProcess) = DatabaseAccess.executor().delete(databaseKey, process)
}