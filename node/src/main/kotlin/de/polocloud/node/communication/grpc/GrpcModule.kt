package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.executer.GrpcExecutor
import de.polocloud.common.communication.registery.GrpcHandlerRegistry
import de.polocloud.node.communication.grpc.middleware.ErrorMiddleware
import de.polocloud.node.communication.grpc.middleware.LoggingMiddleware
import de.polocloud.node.communication.handler.cluster.ListNodesHandler
import de.polocloud.node.communication.handler.node.GetNodeInformationHandler
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.NodeInformationRequest

object GrpcModule {

    fun createExecutor(): GrpcExecutor {
        val registry = GrpcHandlerRegistry().apply {
            register(ListNodesRequest::class.java, ListNodesHandler())
            register(NodeInformationRequest::class.java, GetNodeInformationHandler())
        }

        return GrpcExecutor(
            registry,
            listOf(
                ErrorMiddleware(),
                LoggingMiddleware()
            )
        )
    }
}