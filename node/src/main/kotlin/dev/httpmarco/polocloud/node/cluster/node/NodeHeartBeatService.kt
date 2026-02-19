package dev.httpmarco.polocloud.node.cluster.node

import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.node.cluster.node.data.NodeHeartBeat

class NodeHeartBeatService {

    private val databaseKey = DatabaseKey("nodes_heartbeat", NodeHeartBeat::class)

}