package de.polocloud.cli.communication.connection

import de.polocloud.common.Address
import de.polocloud.common.communication.generator.certificate.CertificateSigningRequestGenerator
import de.polocloud.common.communication.security.toPem
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trError
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterCliRequest
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Performs the one-time CLI registration handshake with the cluster.
 *
 * Registration flow:
 * 1. Opens a **plaintext** channel to the registration port (same port nodes use to register)
 * 2. Sends the registration token + CSR
 * 3. Cluster validates token + IP whitelist, signs the CSR with the CLI CA
 * 4. CLI stores the received signed certificate and CA certificate on disk
 * 5. Channel is closed — all subsequent connections use [CliGrpcChannel] with full mTLS
 *    on the main cluster port
 */
class CliRegistrationClient(
    private val certificateStorage: CliCertificateStorage,
) {

    private val logger = LoggerFactory.getLogger(CliRegistrationClient::class.java)

    /**
     * Connects to [address], registers with [token], and persists received certificates.
     */
    suspend fun register(address: Address, token: String) {
        logger.info(TranslationService.tr(
            "cli",
            "cli.registration.start",
            "address" to address.toString()
        ))

        val channel = openPlaintextChannel(address)

        try {
            val stub = CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineStub(channel)

            val csr = CertificateSigningRequestGenerator(
                certificateStorage.keyPair,
                UUID.randomUUID()
            ).generate()

            val response = try {
                stub.registerCli(
                    RegisterCliRequest.newBuilder()
                        .setToken(token)
                        .setCsrPem(csr.toPem())
                        .build()
                )
            } catch (exception: Exception) {
                logger.trError("cli", "cli.registration.failed", exception, "address" to address)
                return
            }

            if (!response.accepted) {
                logger.trError("cli", "cli.registration.denied","reason" to response.message)
                return
            }

            certificateStorage.saveCertificate(response.certificatePem)
            certificateStorage.saveCaCertificate(response.caCertificatePem)

            logger.info(TranslationService.tr("cli", "cli.registration.success"))

        } finally {
            channel.shutdown()
        }
    }

    private fun openPlaintextChannel(address: Address): ManagedChannel =
        NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .usePlaintext()
            .build()
}