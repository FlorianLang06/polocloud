package de.polocloud.node.core.configuration.cluster

import de.polocloud.common.utils.localIpAddress
import de.polocloud.node.security.CSPRNGGenerator
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
    var enabled: Boolean = true,
    var allowedIps: List<String> = listOf("127.0.0.1", localIpAddress()),
    var registrationToken: String = CSPRNGGenerator.generate()
)
