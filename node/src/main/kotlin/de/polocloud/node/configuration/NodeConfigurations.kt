package de.polocloud.node.configuration

import de.polocloud.common.configuration.ConfigurationHolder

data class NodeConfigurations(
    val cluster: ConfigurationHolder<ClusterConfiguration>,
    val general: ConfigurationHolder<GeneralConfiguration>,
    val localNode: ConfigurationHolder<LocalNodeConfiguration>
) {
    val clusterConfig by cluster
    val generalConfig by general
    val nodeConfig by localNode // TODO better config with envs
}