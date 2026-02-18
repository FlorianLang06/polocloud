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
    val head: Boolean,
    val firstConnection: Long,
    val lastConnection: Long
) {

    fun address(): Address {
        return Address(hostname, port)
    }
}