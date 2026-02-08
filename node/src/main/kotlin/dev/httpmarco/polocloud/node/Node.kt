package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import org.slf4j.LoggerFactory

object Node {
    private val logger = LoggerFactory.getLogger(Node::class.java)
    private val endpoint = GrpcEndpoint(5467)

    init {
        logger.info("Starting Node")
    }
}