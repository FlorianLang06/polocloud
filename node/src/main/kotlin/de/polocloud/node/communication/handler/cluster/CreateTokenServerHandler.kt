package de.polocloud.node.communication.handler.cluster

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.registration.node.token.toProto
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.CreateTokenRequest
import de.polocloud.proto.CreateTokenResponse

class CreateTokenServerHandler : GrpcServerHandler<CreateTokenRequest, CreateTokenResponse> {

    override suspend fun handle(
        request: CreateTokenRequest,
        context: GrpcServerContext
    ): CreateTokenResponse {
        val token = NodeEnvironment.runtime.nodeRegistrationManager.tokenManger.create().toProto()

        return CreateTokenResponse.newBuilder()
            .setToken(token)
            .build()
    }
}