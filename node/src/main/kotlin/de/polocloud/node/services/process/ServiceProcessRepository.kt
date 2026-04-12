package de.polocloud.node.services.process

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey

object ServiceProcessRepository {

    private val databaseKey = DatabaseKey(ServiceProcess::class)

    fun update(process: ServiceProcess) {
        DatabaseAccess.executor().save(databaseKey, process)
    }
}