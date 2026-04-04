package de.polocloud.node.configuration.cluster

import kotlinx.serialization.Serializable

/**
 * Configuration for CLI access to the cluster.
 *
 * Defines which IPs are whitelisted to connect and the registration token
 * required for authentication. All CLI communication runs on the same port
 * as the cluster/node gRPC endpoint.
 */
@Serializable
data class CliAccessConfiguration(
    val enabled: Boolean = true,
    val allowedIps: List<String> = listOf("127.0.0.1"),
    val registrationToken: String = "test" //TODO generate or something like this
)
