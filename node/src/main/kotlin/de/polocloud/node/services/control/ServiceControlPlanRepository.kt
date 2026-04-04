package de.polocloud.node.services.control

import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import java.util.UUID

class ServiceControlPlanRepository(private val connection : DatabaseConnectionFactory<*>) {

    private val databaseKey = DatabaseKey(ServiceControlPlan::class)

    fun findAll() : List<ServiceControlPlan> {
        return this.connection.executor().findAll(this.databaseKey)
    }

    fun localPlans() : List<ServiceControlPlan> {
        return this.findAll().stream().filter { it.requiredOnNode }.toList()
    }

    private fun validLocalNode(plan: ServiceControlPlan, localId : UUID) : Boolean {
        // todo select unique
        return (plan.requiredOnNode ||  plan.nodeWhitelist.split(";").contains(localId.toString()))
    }
}