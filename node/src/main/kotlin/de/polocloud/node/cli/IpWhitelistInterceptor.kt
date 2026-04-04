package de.polocloud.node.cli

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * gRPC interceptor that validates incoming CLI connections against an IP whitelist.
 *
 * This interceptor extracts the remote IP from the gRPC call context and checks
 * it against the configured list of allowed IPs. Connections from non-whitelisted
 * IPs are rejected with PERMISSION_DENIED status.
 *
 * @param allowedIps List of IP addresses that are permitted to connect
 */
class IpWhitelistInterceptor(
    private val allowedIps: List<String>
) : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(IpWhitelistInterceptor::class.java)

    override fun <T, R> interceptCall(
        call: ServerCall<T, R>,
        requestHeaders: Metadata,
        next: ServerCallHandler<T, R>
    ): ServerCall.Listener<T> {

        val remote = call.attributes.get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR)

        if (remote !is InetSocketAddress) {
            logger.warn("CLI connection rejected: unable to determine remote IP")
            call.close(
                Status.PERMISSION_DENIED.withDescription("Unable to determine remote IP"),
                Metadata()
            )
            return object : ServerCall.Listener<T>() {}
        }

        val ip = remote.address.hostAddress

        if (!isAllowed(ip)) {
            logger.warn("CLI connection rejected: IP $ip is not whitelisted")
            call.close(
                Status.PERMISSION_DENIED.withDescription("IP not whitelisted"),
                Metadata()
            )
            return object : ServerCall.Listener<T>() {}
        }

        logger.debug("CLI connection allowed from IP: $ip")
        return next.startCall(call, requestHeaders)
    }

    private fun isAllowed(ip: String): Boolean {
        return allowedIps.any { allowed ->
            val regex = wildcardToRegex(allowed)
            regex.matches(ip)
        }
    }

    /**
     * Converts wildcard patterns to safe regex:
     * 127.0.0.*  -> ^127\.0\.0\..*$
     * *          -> ^.*$
     */
    private fun wildcardToRegex(pattern: String): Regex {
        val escaped = pattern
            .replace(".", "\\.")   // escape dots
            .replace("*", ".*")    // wildcard

        return Regex("^$escaped$")
    }
}
