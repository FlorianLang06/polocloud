package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.node.cluster.external.database.DatabaseIdentifier
import dev.httpmarco.polocloud.node.cluster.external.database.DatabaseKey

val NODE_DATA_KEY = DatabaseKey("nodes", NodeData::class.java)

// TODO: CREATE UNIQUE INDEX unique_head ON nodes (is_head) WHERE is_head = TRUE;
class NodeData(@DatabaseIdentifier val identifier: String, val hostname : String, val port: Int, val head: Boolean = false) {


}