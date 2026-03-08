package de.polocloud.node.node.data

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import java.util.UUID
import kotlin.time.Instant

/**
 * Represents a heartbeat from a node, including usage metrics and TPS.
 *
 * @param id unique identifier of the heartbeat entry
 * @param nodeId the node that sent this heartbeat
 * @param heartBeatAt timestamp of the heartbeat (epoch millis)
 * @param cpuUsage CPU usage in percentage (0.0..100.0)
 * @param memoryUsage Memory usage in percentage (0.0..100.0)
 * @param tps ticks per second of the node
 */

@RepositoryName("nodes_heartbeats")
data class NodeHeartBeat(
    @EntryIdentifier val id: String,
    @EntryRef(clazz = NodeData::class) val nodeId: UUID,
    val heartBeatAt: Instant,
    val cpuUsage: Double,
    val memoryUsage: Double,
    val tps: Double
) {

    init {
        require(cpuUsage in 0.0..100.0) { "CPU usage must be between 0 and 100" }
        require(memoryUsage in 0.0..100.0) { "Memory usage must be between 0 and 100" }
    }

    fun diff(other: NodeHeartBeat) = NodeHeartBeat(
        id, nodeId,
        heartBeatAt,
        cpuUsage - other.cpuUsage,
        memoryUsage - other.memoryUsage,
        tps - other.tps
    )
}