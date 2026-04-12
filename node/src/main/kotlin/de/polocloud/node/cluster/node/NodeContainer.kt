package de.polocloud.node.cluster.node

open class NodeContainer(private val data: NodeData) {

    fun state() : NodeState = this.data.state

    fun isOffline() = (this.data.state == NodeState.OFFLINE)

    fun isStarting() = (this.data.state == NodeState.STARTING)

    fun inShutdownProcess() = (this.data.state == NodeState.STOPPING || this.data.state == NodeState.STOPPED)

}