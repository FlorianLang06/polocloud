package dev.httpmarco.polocloud.node.join

import dev.httpmarco.polocloud.node.node.data.NodeData
import dev.httpmarco.polocloud.node.security.ClusterSecurity
import dev.httpmarco.polocloud.proto.JoinRequest
import dev.httpmarco.polocloud.proto.NodeServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class ClusterNodeApprovalClient(val security : ClusterSecurity) {

    fun requestApproval(
        existingNode: NodeData,
        newNode: NodeData
    ): String {

        val channel = createChannel(existingNode)

        return try {
            val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channel)

            val request = JoinRequest.newBuilder()
                .setNodeId(newNode.id.toString())
                .setPublicKey(newNode.publicKey)
                .build()

            val response = runBlocking {
                stub.requestApproval(request)
            }

            if (!response.approved) {
                throw IllegalStateException("Node rejected join request.")
            }

            response.signature

        } finally {
            shutdownChannel(channel)
        }
    }

    private fun createChannel(node: NodeData): ManagedChannel {
        return NettyChannelBuilder.forAddress(node.hostname, node.port)
            .sslContext(
                GrpcSslContexts.forClient()
                .trustManager(security.certFile())
                .build())
            .build()
    }

    private fun shutdownChannel(channel: ManagedChannel) {
        channel.shutdown()
        channel.awaitTermination(3, TimeUnit.SECONDS)
    }
}