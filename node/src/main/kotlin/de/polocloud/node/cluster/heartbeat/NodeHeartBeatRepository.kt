package de.polocloud.node.cluster.heartbeat

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import de.polocloud.database.filtering.Eq
import java.util.UUID

object NodeHeartBeatRepository {

    private val databaseKey = DatabaseKey(NodeHeartBeat::class)

    fun find(nodeId: UUID) = DatabaseAccess.executor().find(databaseKey, Eq("nodeId", nodeId))

    fun save(beat: NodeHeartBeat) = DatabaseAccess.executor().save(databaseKey, beat)

    fun delete(beat: NodeHeartBeat) = DatabaseAccess.executor().delete(databaseKey, beat)
}