package de.polocloud.node.node

import de.polocloud.node.exception.LocalNodeFindingException
import de.polocloud.node.node.data.NodeData
import de.polocloud.node.repository.NodeRepository
import de.polocloud.node.security.ClusterSecurity
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class NodeStateService(
    private val repository: NodeRepository,
    private val security: ClusterSecurity
) {

    @OptIn(ExperimentalAtomicApi::class)
    private val stateRef = AtomicReference(NodeState.OFFLINE)

    fun markOnline() =
        changeState(NodeState.ONLINE) {
            it == NodeState.STARTING || it == NodeState.SYNCING
        }

    fun markStarting() =
        changeState(NodeState.STARTING, onlyLocal = true) {
            it == NodeState.OFFLINE
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
        if (!predicate(localState())) {
            return
        }

        stateRef.store(newState)

        if (!onlyLocal) {
            val node = findSelf()
            node.state = newState
            repository.save(node)
        }
    }

    private fun findSelf(): NodeData {
        return repository.findNode(security.localId) ?: throw LocalNodeFindingException()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun localState(): NodeState = stateRef.load()
}