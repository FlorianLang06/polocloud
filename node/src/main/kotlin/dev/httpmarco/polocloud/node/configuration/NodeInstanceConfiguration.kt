package dev.httpmarco.polocloud.node.configuration

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.LOCAL_ADDRESS
import dev.httpmarco.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
open class NodeInstanceConfiguration(
    val bindAddress: Address = LOCAL_ADDRESS.withPort(4239),
    val database: DatabaseCredentials,
    val cache: NodeCacheCredentialsConfiguration = NodeCacheCredentialsConfiguration()
) {

}