package de.polocloud.node.configuration

import de.polocloud.common.LOCAL_ADDRESS
import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
data class NodeCacheCredentialsConfiguration(
    val enabled: Boolean = false,
    val redis : DatabaseCredentials.Redis = DatabaseCredentials.Redis(LOCAL_ADDRESS.withPort(6379), "username", "password")
){
}