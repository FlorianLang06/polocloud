package de.polocloud.cli.communication.client.impl.node

import de.polocloud.cli.communication.client.call.node.GetNodeInformationCall
import de.polocloud.common.communication.client.executor.GrpcClientExecutor

class NodeClientImpl(
    private val executor: GrpcClientExecutor
) {

    suspend fun nodeName(): String {
        return executor.execute(GetNodeInformationCall())
    }
}