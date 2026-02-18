package dev.httpmarco.polocloud.node.cluster.node

enum class NodeState {

    OFFLINE,
    STARTING,
    SYNCING,
    ONLINE,
    STOPPING,
    CRASHED

}