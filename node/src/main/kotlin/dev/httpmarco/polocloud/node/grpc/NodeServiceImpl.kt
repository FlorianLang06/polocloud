package dev.httpmarco.polocloud.node.grpc

import dev.httpmarco.polocloud.proto.ApprovalResponse
import dev.httpmarco.polocloud.proto.JoinRequest
import dev.httpmarco.polocloud.proto.NodeServiceGrpcKt

class NodeServiceImpl : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {

    override suspend fun requestApproval(request: JoinRequest): ApprovalResponse {
        // TODO: echte PublicKey Überprüfung
        val approved = true
        val signature = signNodeId(request.nodeId)
        return ApprovalResponse.newBuilder()
            .setApproved(approved)
            .setSignature(signature)
            .build()
    }

    private fun signNodeId(nodeId: String): String {
        return "signed_$nodeId"
    }
}
