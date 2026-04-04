package de.polocloud.node.cli.interceptor

import de.polocloud.node.cli.CliSessionManager
import io.grpc.*
import java.security.cert.X509Certificate

class CliSessionInterceptor(
    private val sessionManager: CliSessionManager
) : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val subject = extractSubject(call)

        if (subject != null) {
            sessionManager.touch(subject)
        }

        return next.startCall(call, headers)
    }

    private fun extractSubject(call: ServerCall<*, *>): String? {
        val sslSession = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
        val cert = sslSession?.peerCertificates?.firstOrNull() as? X509Certificate

        return cert?.subjectX500Principal?.name
    }
}