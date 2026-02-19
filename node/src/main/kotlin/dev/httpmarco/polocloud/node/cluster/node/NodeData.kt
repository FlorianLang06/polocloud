package dev.httpmarco.polocloud.node.cluster.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.EntryIdentifier
import java.util.UUID

data class NodeData(
    @EntryIdentifier val id: UUID = UUID.randomUUID(),
    val name: String,
    val hostname: String,
    val port: Int,
    val state: NodeState,
    val head: Boolean = false,
    val firstConnection: Long = System.currentTimeMillis(),
    val lastConnection: Long = System.currentTimeMillis()
) {

    fun address(): Address {
        return Address(hostname, port)
    }
}