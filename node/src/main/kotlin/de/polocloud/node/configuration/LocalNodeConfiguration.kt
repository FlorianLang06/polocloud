package de.polocloud.node.configuration

import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
data class LocalNodeConfiguration(var database: DatabaseCredentials = DatabaseCredentials.H2("database")) {

}
