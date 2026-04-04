package de.polocloud.common.grpc

import io.grpc.Context

object GrpcClientContext {
    val CLIENT_IP: Context.Key<String> = Context.key("client-ip")
}