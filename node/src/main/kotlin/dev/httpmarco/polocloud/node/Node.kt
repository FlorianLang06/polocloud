package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint

object Node {

    private val endpoint = GrpcEndpoint(5467)

    init {
        endpoint.connect()
    }
}