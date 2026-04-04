package de.polocloud.node.services.control

data class ServiceControlPlan(val name: String, val version: String, val uniqueUse : Boolean, val requiredOnNode : Boolean, val minimum: Boolean, val maximum : Boolean, val nodeWhitelist: String) {
}