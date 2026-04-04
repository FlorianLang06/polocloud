package de.polocloud.node.cli

import de.polocloud.common.certificate.certToPem
import de.polocloud.common.certificate.parseCsr
import de.polocloud.common.grpc.GrpcClientContext
import de.polocloud.common.i18n.trInfo
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cli.interceptor.CliSessionInterceptor
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.security.CertificateAuthority
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.DisconnectRequest
import de.polocloud.proto.DisconnectResponse
import de.polocloud.proto.RegisterCliRequest
import de.polocloud.proto.RegisterCliResponse
import io.grpc.Context
import io.grpc.Grpc
import io.grpc.ServerCall
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.slf4j.LoggerFactory
import java.security.cert.X509Certificate

/**
 * gRPC service for CLI client registration and authentication.
 *
 * Handles the initial registration flow where CLI clients present a registration token
 * and CSR (Certificate Signing Request) to receive a signed certificate for mTLS.
 *
 * Auth flow:
 * 1. CLI sends token + CSR
 * 2. Server validates token against configured registration token
 * 3. Server validates client IP against whitelist
 * 4. Server signs CSR and returns certificate
 * 5. CLI uses certificate for subsequent mTLS requests
 *
 * @param config Cluster configuration containing CLI access settings
 * @param keyPair CA key pair for signing client certificates
 */
class CliRegistrationService(
    private val config: ClusterConfiguration,
    private val certificateDataStorage: CertificateDataStorage,
    private val sessionManager: CliSessionManager
) : CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(CliRegistrationService::class.java)

    /**
     * Lazily loaded persistent CLI CA.
     * Loaded once from disk — not regenerated per request.
     */
    private val cliCa: CertificateAuthority by lazy {
        certificateDataStorage.loadCliCertificateAuthority()
    }

    override suspend fun registerCli(request: RegisterCliRequest): RegisterCliResponse {
        val clientIp = GrpcClientContext.CLIENT_IP.get() ?: "unknown"
        logger.trInfo("cluster", "cluster.registration.cli.starting", "client" to clientIp)

        if (!config.cliAccess.enabled) {
            logger.trInfo("cluster", "cli.access.disabled")
            return denyResponse(TranslationService.tr("cluster","cli.access.disabled"))
        }

        if (request.token != config.cliAccess.registrationToken) {
            logger.trInfo("cluster", "cluster.registration.cli.token.invalid")
            return denyResponse(TranslationService.tr("cluster", "cluster.registration.cli.token.invalid", "client" to clientIp))
        }

        val csr = runCatching { parseCsr(request.csrPem) }.getOrElse {
            logger.trInfo("cluster", "cli.registration.csr.invalid")
            return denyResponse(TranslationService.tr("cluster","cli.registration.csr.invalid"))
        }

        val signedCert = cliCa.signCsr(
            csr,
            subjectAltNames = listOf("cli.polocloud.local") //TODO
        )

        val subject = extractSubject(csr)
        val clientKey = subject.lowercase()
        val session = sessionManager.createOrUpdate(clientKey, clientIp)

        logger.trInfo(
            "cluster",
            "cluster.registration.cli.registered",
            "client" to session.sessionId,
            "ip" to session.address
        )

        return RegisterCliResponse.newBuilder()
            .setAccepted(true)
            .setCertificatePem(certToPem(signedCert))
            .setCaCertificatePem(cliCa.getCaCertificatePem())
            .build()
    }

    override suspend fun disconnectCli(request: DisconnectRequest): DisconnectResponse {
        val subject = CliSessionInterceptor.SUBJECT_CTX_KEY.get()

        if (subject != null) {
            val removed = sessionManager.get(subject.lowercase()) != null

            sessionManager.remove(subject.lowercase())

            if (removed) {
                logger.trInfo(
                    "cluster",
                    "cluster.cli.session.removed",
                    "client" to subject
                )
            } else {
                logger.trInfo(
                    "cluster",
                    "cluster.cli.session.removed.missing",
                    "client" to subject
                )
            }
        }

        return DisconnectResponse.newBuilder().build()
    }

    private fun denyResponse(messageId: String): RegisterCliResponse =
        RegisterCliResponse.newBuilder()
            .setAccepted(false)
            .setMessage(messageId)
            .build()

    private fun extractSubject(csr: PKCS10CertificationRequest): String {
        val x500Name = csr.subject
        val cn = x500Name.getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.first
            ?.value
            ?.toString()

        return cn ?: x500Name.toString()
    }
}
