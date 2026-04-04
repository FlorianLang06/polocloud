package de.polocloud.node.cli

import de.polocloud.common.i18n.trWarn
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.configuration.cluster.CliAccessConfiguration
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
    private val config: CliAccessConfiguration,
) : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(IpWhitelistInterceptor::class.java)

    override fun <T, R> interceptCall(
        call: ServerCall<T, R>,
        requestHeaders: Metadata,
        next: ServerCallHandler<T, R>
    ): ServerCall.Listener<T> {
        val remote = call.attributes.get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR)

        if (!config.enabled) {
            logger.trWarn("cluster", "cli.access.disabled.log", "client" to remote.toString())
            call.close(
                Status.PERMISSION_DENIED.withDescription(TranslationService.tr("cluster", "cli.access.disabled")),
                Metadata()
            )
            return object : ServerCall.Listener<T>() {}
        }

        if (remote !is InetSocketAddress) {
            logger.trWarn("cluster", "cluster.cli.ip.unknown")
            call.close(
                Status.PERMISSION_DENIED.withDescription(TranslationService.tr("cluster", "cluster.cli.ip.unknown")),
                Metadata()
            )
            return object : ServerCall.Listener<T>() {}
        }

        val ip = remote.address.hostAddress

        if (!isAllowed(ip)) {
            logger.trWarn(
                "node",
                "cluster.cli.ip.notAllowed.log",
                "ip" to ip
            )
            call.close(
                Status.PERMISSION_DENIED.withDescription(TranslationService.tr("cluster", "cluster.cli.ip.notAllowed")),
                Metadata()
            )
            return object : ServerCall.Listener<T>() {}
        }

        logger.debug("CLI connection allowed from IP: $ip")
        return next.startCall(call, requestHeaders)
    }

    private fun isAllowed(ip: String): Boolean {
        return config.allowedIps.any { allowed ->
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
