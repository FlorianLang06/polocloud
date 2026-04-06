package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.configuration.ConfigurationFile
import de.polocloud.common.configuration.DefaultableConfiguration
import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
data class LocalNodeConfiguration(var database: DatabaseCredentials) {
    companion object : DefaultableConfiguration<LocalNodeConfiguration> {
        override fun createDefault(): LocalNodeConfiguration {
            val db = DatabaseCredentials.PostgreSQL(
                Address("localhost", 5432),//TODO mirco (set default values or something like that)
                "polocloud",
                "polocloud",
                "polocloud-cluster"
            )
            return LocalNodeConfiguration(db)
        }
    }

}
