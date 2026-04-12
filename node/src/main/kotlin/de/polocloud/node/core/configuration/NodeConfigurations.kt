package de.polocloud.node.core.configuration

import de.polocloud.common.configuration.ConfigurationFile
import kotlinx.serialization.Serializable

@Serializable
@ConfigurationFile("config.json")
data class NodeConfigurations(
    val cluster: ClusterConfiguration = ClusterConfiguration(),
    val general: GeneralConfiguration = GeneralConfiguration(),
    val localNode: LocalNodeConfiguration = LocalNodeConfiguration()
)