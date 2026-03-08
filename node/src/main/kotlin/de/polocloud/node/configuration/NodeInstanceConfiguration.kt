package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCAL_ADDRESS
import de.polocloud.database.DatabaseCredentials
import kotlinx.serialization.Serializable

@Serializable
open class NodeInstanceConfiguration(
    val bindAddress: Address = LOCAL_ADDRESS.withPort(4239),
    val database: DatabaseCredentials,
    val cache: NodeCacheCredentialsConfiguration = NodeCacheCredentialsConfiguration()
) {

}