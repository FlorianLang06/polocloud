package de.polocloud.node.configuration.cluster

import kotlinx.serialization.Serializable

@Serializable
data class CliAccessConfiguration(
    val enabled: Boolean = true,
    val port: Int = 4241,
    val allowedIps: List<String> = emptyList(),
    val registrationToken: String = "test" // TODO generateToken
)
