package de.polocloud.node.communication.registration.cli

import de.polocloud.common.communication.certificate.certToPem
import de.polocloud.common.communication.certificate.parseCsr
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.communication.GrpcClientContext
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.communication.registration.client.CliRegistrationValidator
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.proto.*
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.slf4j.LoggerFactory

/**
 * gRPC service for CLI client registration and disconnection.
 *
 * Auth flow:
 * 1. CLI sends token + CSR
 * 2. [CliRegistrationValidator] checks access flag and token
 * 3. Server signs the CSR via the persistent CLI CA
 * 4. CLI stores the signed certificate and uses it for subsequent mTLS requests
 *
 * The CLI CA is loaded lazily once from disk — never regenerated per request.
 */
class CliRegistrationService(
    holder: ConfigurationHolder<NodeConfigurations>,
    private val sessionManager: ICliSessionManager,
) : CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(CliRegistrationService::class.java)
    private var validator = CliRegistrationValidator(holder)
    private val ca by lazy {
        CertificateDataStorage.certificateAuthority()
    }

    override suspend fun registerCli(request: RegisterCliRequest): RegisterCliResponse {
        val clientIp = GrpcClientContext.CLIENT_IP.get() ?: "unknown"
        logger.trInfo("cluster", "cluster.registration.cli.starting", "client" to clientIp)

        validator.validateAccess().let {
            if (it is CliRegistrationValidator.Result.Denied) return deny(it.translationKey)
        }

        validator.validateToken(request.token).let {
            if (it is CliRegistrationValidator.Result.Denied) return deny(it.translationKey, "client" to clientIp)
        }

        val csr = runCatching { parseCsr(request.csrPem) }.getOrElse {
            logger.trInfo("cluster", "cli.registration.csr.invalid")
            return deny("cli.registration.csr.invalid")
        }

        val signedCert = ca.signCsr(
            csr,
            subjectAltNames = listOf("cli.polocloud.local") // TODO: derive from config
        )

        val subject = extractCn(csr).lowercase()
        val session = sessionManager.createOrUpdate(subject, clientIp)

        logger.trInfo(
            "cluster",
            "cluster.registration.cli.registered",
            "client" to session.sessionId,
            "ip"     to session.address,
        )

        return RegisterCliResponse.newBuilder()
            .setAccepted(true)
            .setCertificatePem(certToPem(signedCert))
            .setCaCertificatePem(ca.getCaCertificatePem())
            .build()
    }

    override suspend fun disconnectCli(request: DisconnectRequest): DisconnectResponse {
        val subject = CliSessionInterceptor.Companion.SUBJECT_CTX_KEY.get()

        if (subject == null) {
            logger.warn("Disconnect received without subject in context — ignoring")
            return DisconnectResponse.newBuilder().build()
        }

        val existed = sessionManager.get(subject.lowercase()) != null
        sessionManager.remove(subject.lowercase())

        val logKey = if (existed) "cluster.cli.session.removed" else "cluster.cli.session.removed.missing"
        logger.trInfo("cluster", logKey, "client" to subject)

        return DisconnectResponse.newBuilder().build()
    }

    private fun deny(translationKey: String, vararg placeholders: Pair<String, Any?>): RegisterCliResponse {
        logger.trInfo("cluster", translationKey, *placeholders)
        return RegisterCliResponse.newBuilder()
            .setAccepted(false)
            .setMessage(TranslationService.tr("cluster", translationKey, *placeholders))
            .build()
    }

    private fun extractCn(csr: PKCS10CertificationRequest): String =
        csr.subject
            .getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.first
            ?.value
            ?.toString()
            ?: csr.subject.toString()
}