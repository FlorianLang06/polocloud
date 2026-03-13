package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCAL_ADDRESS
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.configuration.serializer.LocaleSerializer
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
class NodeConfiguration(
    val database: DatabaseCredentials,
    val general : GeneralConfiguration = GeneralConfiguration(),
    val local: LocalNodeConfiguration = LocalNodeConfiguration(database),
    val cluster : ClusterConfiguration = ClusterConfiguration(),
)

@Serializable
class LocalNodeConfiguration(val database: DatabaseCredentials)

@Serializable
class GeneralConfiguration(
    @Serializable(with = LocaleSerializer::class) val locale: Locale = Locale.US,
    val bindAddress: Address = LOCAL_ADDRESS.withPort(4239),
)

@Serializable
class ClusterConfiguration()

