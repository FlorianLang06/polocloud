package de.polocloud.node.services.process

import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.services.ServiceState
import java.util.UUID

class ServiceProcessRepository(val connection : DatabaseConnectionFactory<*>) {

    private val databaseKey = DatabaseKey(ServiceProcess::class)

    fun generateProcess() : ServiceProcess {
        val process = ServiceProcess(UUID.randomUUID(), -1, -1, ServiceState.LOADING)
        connection.executor().save(databaseKey, process)
        return process
    }
}