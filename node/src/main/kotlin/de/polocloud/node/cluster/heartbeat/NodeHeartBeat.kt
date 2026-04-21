package de.polocloud.node.cluster.heartbeat

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import de.polocloud.node.cluster.node.NodeData
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

    val systemCpuUsage: Double,
    val systemMemoryUsage: Double,

    val applicationCpuUsage: Double,
    val applicationMemoryUsage: Double,

    val tps: Double
) {

    init {
        require(systemCpuUsage in 0.0..100.0 && applicationCpuUsage in 0.0..100.0) { "CPU usage must be between 0 and 100" }
        require(systemMemoryUsage in 0.0..100.0 && applicationMemoryUsage in 0.0..100.0) { "Memory usage must be between 0 and 100" }
    }

    fun diff(other: NodeHeartBeat) = NodeHeartBeat(
        id, nodeId,
        heartBeatAt,
        systemCpuUsage - other.systemCpuUsage,
        systemMemoryUsage - other.systemMemoryUsage,
        applicationCpuUsage - other.applicationCpuUsage,
        applicationMemoryUsage - other.applicationMemoryUsage,
        tps - other.tps
    )
}