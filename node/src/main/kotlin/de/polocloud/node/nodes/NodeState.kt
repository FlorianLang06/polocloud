package de.polocloud.node.nodes

enum class NodeState {

    OFFLINE,
    INITIALIZE,
    STARTING,
    SYNCING,
    ONLINE,
    STOPPING,
    STOPPED,
    CRASHED

}