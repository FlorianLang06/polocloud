package de.polocloud.node.nodes

import kotlin.concurrent.atomics.ExperimentalAtomicApi

class LocalNodeContainer(val data: NodeData) : NodeContainer(data) {

    fun markOnline() =
        changeState(NodeState.ONLINE) {
            it == NodeState.STARTING || it == NodeState.SYNCING
        }

    fun markInitialize() =
        changeState(NodeState.INITIALIZE) {
            it == NodeState.STARTING
        }

    fun markStopping() =
        changeState(NodeState.STOPPING) {
            it == NodeState.ONLINE || it == NodeState.CRASHED
        }

    fun markStopped() =
        changeState(NodeState.STOPPED) {
            it == NodeState.STOPPING || it == NodeState.CRASHED
        }


    @OptIn(ExperimentalAtomicApi::class)
    private fun changeState(
        newState: NodeState,
        onlyLocal: Boolean = false,
        predicate: (NodeState) -> Boolean
    ) {
        if (!predicate(this.state())) {
            return
        }

        data.state = newState

        //   repository.save(node)
    }
}