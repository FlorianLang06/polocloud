package de.polocloud.node.core.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCALHOST_ADDRESS
import de.polocloud.node.core.configuration.cluster.CliAccessConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ClusterConfiguration(
    var registration: Address = LOCALHOST_ADDRESS.withPort(4240),
    val cliAccess: CliAccessConfiguration = CliAccessConfiguration()
)
