package de.polocloud.node.core.configuration

import de.polocloud.common.configuration.ConfigurationFile
import kotlinx.serialization.Serializable

@Serializable
@ConfigurationFile("config.json")
data class NodeConfigurations(
    var cluster: ClusterConfiguration = ClusterConfiguration(),
    var general: GeneralConfiguration = GeneralConfiguration(),
    var localNode: LocalNodeConfiguration = LocalNodeConfiguration()
)