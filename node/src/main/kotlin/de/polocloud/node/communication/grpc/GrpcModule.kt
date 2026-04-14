package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.common.communication.server.registery.GrpcServerHandlerRegistry
import de.polocloud.node.communication.grpc.middleware.ErrorServerMiddleware
import de.polocloud.node.communication.grpc.middleware.LoggingServerMiddleware
import de.polocloud.node.communication.handler.cluster.ListNodesServerHandler
import de.polocloud.node.communication.handler.node.GetNodeInformationServerHandler
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.NodeInformationRequest

object GrpcModule {

    fun createExecutor(): GrpcServerExecutor {
        val registry = GrpcServerHandlerRegistry().apply {
            register(ListNodesRequest::class.java, ListNodesServerHandler())
            register(NodeInformationRequest::class.java, GetNodeInformationServerHandler())
        }

        return GrpcServerExecutor(
            registry,
            listOf(
                ErrorServerMiddleware(),
                LoggingServerMiddleware()
            )
        )
    }
}