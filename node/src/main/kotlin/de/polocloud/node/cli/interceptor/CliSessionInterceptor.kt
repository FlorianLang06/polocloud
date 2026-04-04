package de.polocloud.node.cli.interceptor

import de.polocloud.node.cli.CliSessionManager
import io.grpc.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.net.InetSocketAddress
import java.security.cert.X509Certificate

class CliSessionInterceptor(
    private val sessionManager: CliSessionManager
) : ServerInterceptor {

    companion object {
        val SUBJECT_CTX_KEY: Context.Key<String> = Context.key("cli-subject")
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val subject = extractSubject(call)
        val ip = extractIp(call)

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
        val sslSession = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
        val cert = sslSession?.peerCertificates?.firstOrNull() as? X509Certificate ?: return null

        val x500 = X500Name(cert.subjectX500Principal.name)

        return x500.getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.first
            ?.value
            ?.toString()
            ?.lowercase()
    }

    private fun extractIp(call: ServerCall<*, *>): String? {
        val remote = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
        return (remote as? InetSocketAddress)?.address?.hostAddress
    }
}