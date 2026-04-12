package de.polocloud.node.cluster.node

enum class NodeState {

    OFFLINE,
    STARTING,
    SYNCING,
    ONLINE,
    STOPPING,
    STOPPED,
    CRASHED

}