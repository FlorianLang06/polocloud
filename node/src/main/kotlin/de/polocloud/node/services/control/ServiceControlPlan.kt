package de.polocloud.node.services.control

import de.polocloud.database.EntryIdentifier

data class ServiceControlPlan(@EntryIdentifier val name: String, val version: String, val uniqueUse : Boolean, val requiredOnNode : Boolean, val minimum: Int, val maximum : Int, val nodeWhitelist: String) {
}