package de.polocloud.node.cluster.node

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoNodeData
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@RepositoryName("nodes")
data class NodeData(
    @EntryIdentifier val id : UUID,
    val index: Int,
    val groupName: String = "node",
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
        return "$groupName-$index"
    }
}

fun NodeData.toProto(): ProtoNodeData {
    return ProtoNodeData.newBuilder()
        .setId(this.id.toString())
        .setIndex(this.index)
        .setGroupName(this.groupName)
        .setHostname(this.hostname)
        .setPort(this.port)
        .setState(this.state)
        .setHead(this.head)
        .setVersion(this.version)
        .setGitCommitHash(this.gitCommitHash)
        .setFirstConnection(this.firstConnection.toEpochMilliseconds())
        .setLastConnection(this.lastConnection.toEpochMilliseconds())
        .build()
}