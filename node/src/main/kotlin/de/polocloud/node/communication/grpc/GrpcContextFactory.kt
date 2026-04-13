package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.GrpcClientContext
import de.polocloud.node.communication.interceptor.CliSessionInterceptor

object GrpcContextFactory {

    fun fromGrpc(): GrpcContext {
        return GrpcContext()
            .with("clientIp", GrpcClientContext.CLIENT_IP.get())
            .with("subject", CliSessionInterceptor.SUBJECT_CTX_KEY.get())
    }
}