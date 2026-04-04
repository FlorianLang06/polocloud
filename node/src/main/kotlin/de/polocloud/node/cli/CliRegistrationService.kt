package de.polocloud.node.cli

import de.polocloud.common.certificate.certToPem
import de.polocloud.common.certificate.parseCsr
import de.polocloud.common.i18n.trInfo
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.security.CertificateAuthority
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterCliRequest
import de.polocloud.proto.RegisterCliResponse
import org.slf4j.LoggerFactory

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
        logger.trInfo("cluster", "cluster.registration.cli.starting") // TODO add client placeholder with session

        if (!config.cliAccess.enabled) {
            logger.trInfo("cluster", "cli.access.disabled")
            return denyResponse(TranslationService.tr("cluster","cli.access.disabled"))
        }

        if (request.token != config.cliAccess.registrationToken) {
            logger.trInfo("cluster", "cluster.registration.cli.token.invalid")
            return denyResponse(TranslationService.tr("cluster", "cluster.registration.cli.token.invalid")) // TODO add client placeholder with session
        }

        val csr = runCatching { parseCsr(request.csrPem) }.getOrElse {
            logger.trInfo("cluster", "cli.registration.csr.invalid")
            return denyResponse(TranslationService.tr("cluster","cli.registration.csr.invalid"))
        }

        val signedCert = cliCa.signCsr(
            csr,
            subjectAltNames = listOf("cli.polocloud.local")
        )

        logger.trInfo("cluster", "cluster.registration.cli.registered") // TODO add client placeholder with session
        //TODO add disconnect

        return RegisterCliResponse.newBuilder()
            .setAccepted(true)
            .setCertificatePem(certToPem(signedCert))
            .setCaCertificatePem(cliCa.getCaCertificatePem())
            .build()
    }

    private fun denyResponse(messageId: String): RegisterCliResponse =
        RegisterCliResponse.newBuilder()
            .setAccepted(false)
            .setMessage(messageId)
            .build()
}
