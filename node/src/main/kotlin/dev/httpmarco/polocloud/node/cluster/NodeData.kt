package dev.httpmarco.polocloud.node.cluster

// TODO: CREATE UNIQUE INDEX unique_head ON nodes (is_head) WHERE is_head = TRUE;
class NodeData(val identifier: String, val hostname : String, val port: Int, val head: Boolean = false) {


}