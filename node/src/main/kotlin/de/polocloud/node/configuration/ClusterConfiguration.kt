package de.polocloud.node.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCALHOST_ADDRESS
import de.polocloud.common.configuration.ConfigurationFile
import de.polocloud.node.configuration.cluster.CliAccessConfiguration
import kotlinx.serialization.Serializable

@Serializable
@ConfigurationFile("cluster.json")
data class ClusterConfiguration(
    var registration: Address = LOCALHOST_ADDRESS.withPort(4240),
    val cliAccess: CliAccessConfiguration = CliAccessConfiguration()
)
