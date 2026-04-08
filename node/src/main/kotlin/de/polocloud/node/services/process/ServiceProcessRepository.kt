package de.polocloud.node.services.process

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.LOCAL_ID
import de.polocloud.node.services.ServiceState
import de.polocloud.node.services.control.ServiceControlPlan
import java.util.UUID
import javax.xml.crypto.Data

object ServiceProcessRepository {

    private val databaseKey = DatabaseKey(ServiceProcess::class)

    fun update(process: ServiceProcess) {
        DatabaseAccess.executor().save(databaseKey, process)
    }
}