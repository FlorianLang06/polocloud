package de.polocloud.node.cluster.node

import de.polocloud.proto.NodeState
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class LocalNodeContainer(val data: NodeData) : NodeContainer(data) {

    fun markOnline() =
        changeState(NodeState.ONLINE) {
            it == NodeState.STARTING || it == NodeState.SYNCING
        }

    fun markStarting() =
        changeState(NodeState.STARTING) {
           true
        }

    fun markStopping() =
        changeState(NodeState.STOPPING) {
            it == NodeState.ONLINE || it == NodeState.CRASHED || it == NodeState.STARTING
        }

    fun markStopped() =
        changeState(NodeState.STOPPED) {
            it == NodeState.STOPPING || it == NodeState.CRASHED
        }


    @OptIn(ExperimentalAtomicApi::class)
    private fun changeState(
        newState: NodeState,
        predicate: (NodeState) -> Boolean
    ) {
        if (!predicate(this.state())) {
            return
        }

        data.state = newState
        NodeRepository.save(this.data)
    }
}