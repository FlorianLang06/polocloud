package de.polocloud.cli.communication.client.impl.cluster

import de.polocloud.cli.communication.client.call.cluster.CreateTokenCall
import de.polocloud.cli.communication.client.call.cluster.ListNodesCall
import de.polocloud.common.communication.client.executor.GrpcClientExecutor
import de.polocloud.node.communication.registration.node.token.ProtoRegistrationToken
import de.polocloud.proto.ProtoNodeData

class ClusterClientImpl(
    private val executor: GrpcClientExecutor
) {

    suspend fun createToken(): ProtoRegistrationToken {
        return executor.execute(CreateTokenCall())
    }

    suspend fun createToken(ttlMs: Long): ProtoRegistrationToken {
        return executor.execute(CreateTokenCall(ttlMs))
    }

    suspend fun listNodes(): List<ProtoNodeData> {
        return executor.execute(ListNodesCall())
    }
}