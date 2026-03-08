package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCAL_ADDRESS
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.configuration.serializer.LocaleSerializer
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
open class NodeInstanceConfiguration(
    @Serializable(with = LocaleSerializer::class)
    val language: Locale = Locale.ENGLISH,
    val bindAddress: Address = LOCAL_ADDRESS.withPort(4239),
    val database: DatabaseCredentials,
    val cache: NodeCacheCredentialsConfiguration = NodeCacheCredentialsConfiguration()
) {

}