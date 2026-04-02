package de.polocloud.node.cli

import io.grpc.Grpc
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import io.grpc.Metadata

class IpWhitelistInterceptor(private val allowedIps: List<String>) : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun <Q : Any, R : Any> interceptCall(
        call: ServerCall<Q, R>,
        headers: Metadata,
        next: ServerCallHandler<Q, R>
    ): ServerCall.Listener<Q> {
        val remoteAddr = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
        val ip = (remoteAddr as? InetSocketAddress)?.address?.hostAddress

        if (ip == null || ip !in allowedIps) {
            logger.warn("Rejected connection from: $ip — not in CLI whitelist")
            call.close(
                Status.PERMISSION_DENIED.withDescription("IP not whitelisted: $ip"),
                Metadata()
            )
            return object : ServerCall.Listener<Q>() {}
        }

        return next.startCall(call, headers)
    }
}