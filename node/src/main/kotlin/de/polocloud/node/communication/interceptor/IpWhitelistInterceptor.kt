package de.polocloud.node.communication.interceptor

import de.polocloud.common.grpc.GrpcClientContext
import de.polocloud.common.i18n.trWarn
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.core.configuration.cluster.CliAccessConfiguration
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
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
 * Rejects connections from non-whitelisted IPs with [Status.PERMISSION_DENIED].
 * Wildcard patterns (e.g. `127.0.0.*`) are supported.
 *
 * @param config CLI access configuration containing the allowed IP list
 */
class IpWhitelistInterceptor(
    private val config: CliAccessConfiguration,
) : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(IpWhitelistInterceptor::class.java)

    override fun <T, R> interceptCall(
        call: ServerCall<T, R>,
        headers: Metadata,
        next: ServerCallHandler<T, R>,
    ): ServerCall.Listener<T> {
        if (!config.enabled) {
            val remote = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString()
            logger.trWarn("cluster", "cli.access.disabled.log", "client" to remote)
            return deny(call, "cli.access.disabled")
        }

        val remote = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)

        if (remote !is InetSocketAddress) {
            logger.trWarn("cluster", "cluster.cli.ip.unknown")
            return deny(call, "cluster.cli.ip.unknown")
        }

        val ip = remote.address.hostAddress

        if (!isAllowed(ip)) {
            logger.trWarn("cluster", "cluster.cli.ip.notAllowed.log", "ip" to ip)
            return deny(call, "cluster.cli.ip.notAllowed")
        }

        logger.debug("CLI connection allowed from IP: $ip")
        val context = Context.current().withValue(GrpcClientContext.CLIENT_IP, ip)
        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun isAllowed(ip: String): Boolean =
        config.allowedIps.any { wildcardToRegex(it).matches(ip) }

    /**
     * Converts a wildcard pattern to a regex.
     *
     * Examples:
     * - `127.0.0.*` → `^127\.0\.0\..*$`
     * - `*`         → `^.*$`
     */
    private fun wildcardToRegex(pattern: String): Regex {
        val escaped = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
        return Regex("^$escaped$")
    }

    private fun <T, R> deny(
        call: ServerCall<T, R>,
        translationKey: String,
    ): ServerCall.Listener<T> {
        call.close(
            Status.PERMISSION_DENIED.withDescription(
                TranslationService.tr("cluster", translationKey)
            ),
            Metadata(),
        )
        return object : ServerCall.Listener<T>() {}
    }
}
