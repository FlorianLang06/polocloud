package de.polocloud.cli.communication.client.call.cluster

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.node.communication.registration.node.token.ProtoRegistrationToken
import de.polocloud.proto.ClusterServiceGrpcKt
import de.polocloud.proto.CreateTokenRequest
import io.grpc.ManagedChannel

class CreateTokenCall(
    val ttlMs: Long? = null,
) : GrpcClientCall<ProtoRegistrationToken> {

    override suspend fun execute(channel: ManagedChannel): ProtoRegistrationToken {
        val stub = ClusterServiceGrpcKt.ClusterServiceCoroutineStub(channel)

        val request = CreateTokenRequest.newBuilder()
        if (ttlMs != null) {
            request.setTtlMs(ttlMs)
        }

        val response = stub.createToken(request.build())
        return response.token
    }


}