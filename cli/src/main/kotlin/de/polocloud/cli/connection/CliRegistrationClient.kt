package de.polocloud.cli.connection

import de.polocloud.common.Address
import de.polocloud.common.generator.CertificateSigningRequestGenerator
import de.polocloud.common.security.toPem
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterCliRequest
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory
import java.util.UUID

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
     *
     * @throws CliRegistrationException if the cluster rejects the registration
     * @throws io.grpc.StatusException on gRPC transport errors
     */
    suspend fun register(address: Address, token: String) {
        logger.info("Starting CLI registration with cluster at $address")

        val channel = openPlaintextChannel(address)
        try {
            val stub = CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineStub(channel)

            val csr = CertificateSigningRequestGenerator(
                certificateStorage.keyPair,
                UUID.randomUUID()
            ).generate()

            val response = stub.registerCli(
                RegisterCliRequest.newBuilder()
                    .setToken(token)
                    .setCsrPem(csr.toPem())
                    .build()
            )

            if (!response.accepted) {
                throw CliRegistrationException(
                    "Cluster rejected CLI registration: ${response.message}"
                )
            }

            certificateStorage.saveCertificate(response.certificatePem)
            certificateStorage.saveCaCertificate(response.caCertificatePem)

            logger.info("CLI registration successful — certificates stored")
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

/** Thrown when the cluster explicitly rejects a CLI registration request. */
class CliRegistrationException(message: String) : Exception(message)