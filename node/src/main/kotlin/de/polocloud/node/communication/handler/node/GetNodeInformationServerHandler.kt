package de.polocloud.node.communication.handler.node

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse

class GetNodeInformationServerHandler : GrpcServerHandler<NodeInformationRequest, NodeInformationResponse> {

    override suspend fun handle(
        request: NodeInformationRequest,
        context: GrpcServerContext
    ): NodeInformationResponse {
        val node = NodeEnvironment.runtime.identityService.container.data

        return NodeInformationResponse.newBuilder()
            .setNodeName(node.name())
            .build()
    }
}