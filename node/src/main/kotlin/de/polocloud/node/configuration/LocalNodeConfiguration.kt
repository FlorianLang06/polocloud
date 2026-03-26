package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.configuration.ConfigFile
import de.polocloud.common.configuration.DefaultableConfig
import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
@ConfigFile("local-node.json")
data class LocalNodeConfiguration(var database: DatabaseCredentials) {
    companion object : DefaultableConfig<LocalNodeConfiguration> {
        override fun createDefault(): LocalNodeConfiguration {
            val db = DatabaseCredentials.PostgreSQL(
                Address("localhost", 5432),//TODO mirco (set default values or something like that)
                "user",
                "password",
                "database"
            )
            return LocalNodeConfiguration(db)
        }
    }

}
