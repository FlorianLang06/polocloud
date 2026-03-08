package dev.httpmarco.polocloud.node.node.data

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.EntryIdentifier
import dev.httpmarco.polocloud.database.RepositoryName
import dev.httpmarco.polocloud.node.node.NodeState
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Represents a node in the PoloCloud cluster.
 *
 * @param id Unique identifier of the node
 * @param name Node name
 * @param hostname Node hostname or IP
 * @param port Node port
 * @param state Current node state
 * @param head True if this node is a head node
 * @param publicKey Public key for secure communication
 * @param version Node software version
 * @param gitCommitHash Git commit hash of the node software
 * @param firstConnection Timestamp of first connection
 * @param lastConnection Timestamp of last connection
 */

@RepositoryName("nodes")
data class NodeData(
    @EntryIdentifier val id: UUID = UUID.randomUUID(),
    val index: Int,
    val hostname: String,
    val port: Int,
    var state: NodeState,
    val head: Boolean = false,
    val publicKey: String,
    val version: String,
    val gitCommitHash: String,
    val firstConnection: Instant = Clock.System.now(),
    var lastConnection: Instant = Clock.System.now()
) {

    init {
        require(port in 0..65535) { "Port must be between 0 and 65535" }
    }

    fun address(): Address {
        return Address(hostname, port)
    }

    fun isOnline() = state == NodeState.ONLINE

    fun name() : String {
        return "node-$index"
    }
}