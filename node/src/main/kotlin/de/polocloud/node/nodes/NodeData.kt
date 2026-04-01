package de.polocloud.node.nodes

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@RepositoryName("nodes")
data class NodeData(
    @EntryIdentifier val id: UUID = UUID.randomUUID(),
    val index: Int,
    val hostname: String,
    val port: Int,
    var state: NodeState,
    val head: Boolean = false,
    val version: String,
    val gitCommitHash: String,
    val firstConnection: Instant = Clock.System.now(),
    var lastConnection: Instant = Clock.System.now()
) {

    fun name(): String {
        return "node-$index"
    }
}