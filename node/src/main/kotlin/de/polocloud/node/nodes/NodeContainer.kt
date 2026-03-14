package de.polocloud.node.nodes

open class NodeContainer(private val data: NodeData) {

    fun state() : NodeState = this.data.state

    fun update() {
        // todo
    }

    fun isOffline() = (this.data.state == NodeState.OFFLINE)

    fun isInitialize() = (this.data.state == NodeState.INITIALIZE)

    fun inShutdownProcess() = (this.data.state == NodeState.STOPPING || this.data.state == NodeState.STOPPED)

}