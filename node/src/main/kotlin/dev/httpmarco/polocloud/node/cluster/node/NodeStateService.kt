package dev.httpmarco.polocloud.node.cluster.node

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.node.cluster.exception.LocalNodeFindingException
import dev.httpmarco.polocloud.node.cluster.node.data.NodeData
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class NodeStateService(
    private val database: DatabaseConnectionFactory<*>,
    private val security: ClusterSecurity
) {

    private val clusterKey = DatabaseKey(NodeData::class)
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
        predicate: (NodeData) -> Boolean
    ) {
        val node = findSelf()

        if (!predicate(node)) return

        stateRef.store(newState)
        node.state = newState
        database.executor().save(clusterKey, node)
    }

    private fun findSelf(): NodeData {
        return database.executor().findById(clusterKey, security.localId) ?: throw LocalNodeFindingException()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun localState() : NodeState = stateRef.load()
}