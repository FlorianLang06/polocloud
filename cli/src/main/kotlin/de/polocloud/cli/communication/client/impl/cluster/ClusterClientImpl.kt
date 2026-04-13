package de.polocloud.cli.communication.client.impl.cluster

import de.polocloud.cli.communication.client.call.cluster.ListNodesCall
import de.polocloud.common.communication.client.executor.GrpcClientExecutor
import de.polocloud.proto.ProtoNodeData

class ClusterClientImpl(
    private val executor: GrpcClientExecutor
) {

    suspend fun listNodes(): List<ProtoNodeData> {
        return executor.execute(ListNodesCall())
    }
}