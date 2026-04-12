package de.polocloud.node.communication.interceptor

import de.polocloud.node.communication.cli.session.ICliSessionManager
import io.grpc.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.net.InetSocketAddress
import java.security.cert.X509Certificate

/**
 * gRPC interceptor that extracts the CLI client's certificate subject (CN)
 * and keeps the [ICliSessionManager] up to date on every inbound call.
 *
 * The extracted subject is published via [SUBJECT_CTX_KEY] so downstream
 * handlers (e.g. disconnect) can identify the caller without re-parsing the cert.
 */
class CliSessionInterceptor(
    private val sessionManager: ICliSessionManager,
) : ServerInterceptor {

    companion object {
        val SUBJECT_CTX_KEY: Context.Key<String> = Context.key("cli-subject")
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val subject = extractSubject(call)
        val ip      = extractIp(call)

        if (subject != null && ip != null) {
            sessionManager.createOrUpdate(subject, ip)
        }

        val context = if (subject != null) {
            Context.current().withValue(SUBJECT_CTX_KEY, subject)
        } else {
            Context.current()
        }

        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun extractSubject(call: ServerCall<*, *>): String? {
        val cert = call.attributes
            .get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
            ?.peerCertificates
            ?.firstOrNull() as? X509Certificate
            ?: return null

        return X500Name(cert.subjectX500Principal.name)
            .getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.first
            ?.value
            ?.toString()
            ?.lowercase()
    }

    private fun extractIp(call: ServerCall<*, *>): String? =
        (call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR) as? InetSocketAddress)
            ?.address
            ?.hostAddress
}