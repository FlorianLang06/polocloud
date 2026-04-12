package de.polocloud.node.core.configuration

import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
data class LocalNodeConfiguration(var database: DatabaseCredentials = DatabaseCredentials.H2("database")) {

}
