package dev.httpmarco.polocloud.node.node

import dev.httpmarco.polocloud.node.security.toBase64

object NodeFactory {

    fun createInitial(security: dev.httpmarco.polocloud.node.security.ClusterSecurity, ip: String, port: Int): dev.httpmarco.polocloud.node.node.data.NodeData =
        create(security, 1, ip, head = true, port = port)

    fun create(
        security: dev.httpmarco.polocloud.node.security.ClusterSecurity,
        index: Int,
        ip: String,
        port: Int,
        head: Boolean = false
    ): dev.httpmarco.polocloud.node.node.data.NodeData =
        _root_ide_package_.dev.httpmarco.polocloud.node.node.data.NodeData(
            id = security.localId,
            index = index,
            hostname = ip,
            port = port,
            state = NodeState.STARTING,
            publicKey = security.publicKey.toBase64(),
            head = head,
            version = "1.0.0",
            gitCommitHash = "unknown"
        )
}