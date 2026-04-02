package de.polocloud.node.cli

import de.polocloud.common.certificate.certToPem
import de.polocloud.common.certificate.parseCsr
import de.polocloud.node.configuration.cluster.CliAccessConfiguration
import de.polocloud.node.generator.SelfSignedCertificateGenerator
import de.polocloud.node.security.CertificateAuthority
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterCliRequest
import de.polocloud.proto.RegisterCliResponse
import org.slf4j.LoggerFactory
import java.security.KeyPair

class CliRegistrationService(
    private val config: CliAccessConfiguration,
    private val keyPair: KeyPair,
) : CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun registerCli(request: RegisterCliRequest): RegisterCliResponse {
        if (config.registrationToken != request.token) {
            logger.warn("CLI registration rejected: invalid token")
            return deny("cli.registration.token.invalid")
        }

        val caCert = SelfSignedCertificateGenerator(keyPair).generate()
        val ca = CertificateAuthority(keyPair, caCert)
        val csr = parseCsr(request.csrPem)
        val cert = ca.signCsr(csr)

        logger.info("CLI registered successfully")

        return RegisterCliResponse.newBuilder()
            .setAccepted(true)
            .setCertificatePem(certToPem(cert))
            .setCaCertificatePem(ca.getCaCertificatePem())
            .build()
    }

    private fun deny(messageId: String) = RegisterCliResponse.newBuilder()
        .setAccepted(false)
        .setMessage(messageId)
        .build()
}