package dev.httpmarco.polocloud.node.node

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class NodeStateService(
    private val repository: dev.httpmarco.polocloud.node.repository.NodeRepository,
    private val security: dev.httpmarco.polocloud.node.security.ClusterSecurity
) {

    @OptIn(ExperimentalAtomicApi::class)
    private val stateRef = AtomicReference(NodeState.OFFLINE)

    fun markOnline() =
        changeState(NodeState.ONLINE) {
            it.state == NodeState.STARTING || it.state == NodeState.SYNCING
        }

    fun markStopping() =
        changeState(NodeState.STOPPING) {
            it.state == NodeState.ONLINE || it.state == NodeState.CRASHED
        }

    fun markStopped() =
        changeState(NodeState.STOPPED) {
            it.state == NodeState.STOPPING || it.state == NodeState.CRASHED
        }

    @OptIn(ExperimentalAtomicApi::class)
    private fun changeState(
        newState: NodeState,
        predicate: (dev.httpmarco.polocloud.node.node.data.NodeData) -> Boolean
    ) {
        val node = findSelf()

        if (!predicate(node)) return

        stateRef.store(newState)
        node.state = newState
        repository.save(node)
    }

    private fun findSelf(): dev.httpmarco.polocloud.node.node.data.NodeData {
        return repository.findNode(security.localId) ?: throw _root_ide_package_.dev.httpmarco.polocloud.node.exception.LocalNodeFindingException()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun localState(): NodeState = stateRef.load()
}