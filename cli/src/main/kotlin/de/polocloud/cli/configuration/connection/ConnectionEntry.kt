package de.polocloud.cli.configuration.connection

import de.polocloud.common.Address
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionEntry(
    val clusterAddress: Address,
    val registrationAddress: Address,
    val lastConnected: Long = System.currentTimeMillis(),
)