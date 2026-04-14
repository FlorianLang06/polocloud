package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.GrpcClientContext
import de.polocloud.node.communication.interceptor.CliSessionInterceptor

object GrpcContextFactory {

    fun fromGrpc(): GrpcServerContext {
        return GrpcServerContext()
            .with("clientIp", GrpcClientContext.CLIENT_IP.get())
            .with("subject", CliSessionInterceptor.SUBJECT_CTX_KEY.get())
    }
}