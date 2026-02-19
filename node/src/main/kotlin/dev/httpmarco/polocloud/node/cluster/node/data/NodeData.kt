package dev.httpmarco.polocloud.node.cluster.node.data

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.EntryIdentifier
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import java.util.UUID

data class NodeData(
    @EntryIdentifier val id: UUID = UUID.randomUUID(),
    val name: String,
    val hostname: String,
    val port: Int,
    var state: NodeState,
    val head: Boolean = false,
    val publicKey: String,
    val version: String,
    val gitCommitHash: String,
    val firstConnection: Long = System.currentTimeMillis(),
    val lastConnection: Long = System.currentTimeMillis()
) {

    fun address(): Address {
        return Address(hostname, port)
    }
}