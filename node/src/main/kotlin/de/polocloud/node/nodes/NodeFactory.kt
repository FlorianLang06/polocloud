package de.polocloud.node.nodes

import de.polocloud.common.Address
import de.polocloud.common.version.PolocloudVersion
import java.util.UUID

object NodeFactory {

    fun createInitial(id : UUID, address: Address): NodeData =
        create(id, 1, address, head = true)

    fun create(
        id: UUID,
        index: Int,
        address: Address,
        head: Boolean = false
    ): NodeData =
        NodeData(
            id = id,
            index = index,
            hostname = address.hostname,
            port = address.port,
            state = NodeState.STARTING,
            head = head,
            version = PolocloudVersion.CURRENT.toVersionString(),
            gitCommitHash = PolocloudVersion.CURRENT.commitId
        )
}