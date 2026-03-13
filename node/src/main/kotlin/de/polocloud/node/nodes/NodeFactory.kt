package de.polocloud.node.nodes

import de.polocloud.common.Address

object NodeFactory {

    fun createInitial(address: Address): NodeData =
        create(1, address, head = true)

    fun create(
        index: Int,
        address: Address,
        head: Boolean = false
    ): NodeData =
        NodeData(
            index = index,
            hostname = address.hostname,
            port = address.port,
            state = NodeState.STARTING,
            publicKey = "",
            head = head,
            version = "1.0.0",
            gitCommitHash = "unknown"
        )
}