package de.polocloud.node.communication.handler.node

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.handler.GrpcHandler
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse

class GetNodeInformationHandler : GrpcHandler<NodeInformationRequest, NodeInformationResponse> {

    override suspend fun handle(
        request: NodeInformationRequest,
        context: GrpcContext
    ): NodeInformationResponse {
        val node = NodeEnvironment.runtime.identityService.container.data

        return NodeInformationResponse.newBuilder()
            .setNodeName(node.name())
            .build()
    }
}