package de.polocloud.node.configuration

import de.polocloud.common.configuration.ConfigHolder

data class NodeConfigurations(
    val cluster: ConfigHolder<ClusterConfiguration>,
    val general: ConfigHolder<GeneralConfiguration>,
    val localNode: ConfigHolder<LocalNodeConfiguration>
) {
    val clusterConfig by cluster
    val generalConfig by general
    val nodeConfig by localNode // TODO better config with envs
}