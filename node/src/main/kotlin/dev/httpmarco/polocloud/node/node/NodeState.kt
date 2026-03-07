package dev.httpmarco.polocloud.node.node

enum class NodeState {

    OFFLINE,
    STARTING,
    SYNCING,
    ONLINE,
    STOPPING,
    STOPPED,
    CRASHED

}