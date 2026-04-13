package de.polocloud.node.cluster.node

import de.polocloud.common.Address
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.NodeState
import java.util.*

object NodeFactory {

    fun createInitial(address: Address, group: String): NodeData =
        create(NodeEnvironment.runtime.nodeId.get(), 1, group, address, true)

    fun create(
        id: UUID,
        index: Int,
        groupName: String,
        address: Address,
        head: Boolean = false
    ): NodeData =
        NodeData(
            id = id,
            index = index,
            groupName = groupName,
            hostname = address.hostname,
            port = address.port,
            state = NodeState.STARTING,
            head = head,
            version = PolocloudVersion.CURRENT.toVersionString(),
            gitCommitHash = PolocloudVersion.CURRENT.commitId
        )
}