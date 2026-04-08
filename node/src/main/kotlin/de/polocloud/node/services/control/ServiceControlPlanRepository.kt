package de.polocloud.node.services.control

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.services.ServiceHolder
import java.util.UUID

object ServiceControlPlanRepository {

    private val databaseKey = DatabaseKey(ServiceControlPlan::class)

    fun findAll() : List<ServiceControlPlan> {
        return DatabaseAccess.executor().findAll(this.databaseKey)
    }

    fun findById(name : String) : ServiceControlPlan? {
        return DatabaseAccess.executor().findById(this.databaseKey, name)
    }

    fun generateDefault(holder: ServiceHolder) {
        return DatabaseAccess.executor().save(this.databaseKey, ServiceControlPlan(holder.name, holder.version,
            uniqueUse = false,
            requiredOnNode = true,
            minimum = 1,
            maximum = 1,
            nodeWhitelist = ""
        ))
    }

    fun localPlans() : List<ServiceControlPlan> {
        return this.findAll().stream().filter { it.requiredOnNode }.toList()
    }

    private fun validLocalNode(plan: ServiceControlPlan, localId : UUID) : Boolean {
        // todo select unique
        return (plan.requiredOnNode ||  plan.nodeWhitelist.split(";").contains(localId.toString()))
    }
}