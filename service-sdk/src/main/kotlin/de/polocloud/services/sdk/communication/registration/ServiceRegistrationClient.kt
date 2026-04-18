package de.polocloud.services.sdk.communication.registration

import de.polocloud.common.Address
import de.polocloud.common.communication.generator.certificate.CertificateSigningRequestGenerator
import de.polocloud.common.communication.security.toPem
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.proto.RegisterServiceRequest
import de.polocloud.proto.ServiceRegistrationServiceGrpcKt
import de.polocloud.services.sdk.communication.security.ServiceCertificateStorage
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Performs the one-time service registration handshake with the node.
 *
 * Registration flow (mirrors [CliRegistrationClient]):
 * 1. Opens a **plaintext** channel to the node's registration port
 * 2. Generates a CSR from [ServiceCertificateStorage.keyPair]
 * 3. Sends `serviceId` + `token` + CSR
 * 4. Node validates token (single-use, bound to serviceId), signs CSR
 * 5. Service persists the signed cert + CA cert via [ServiceCertificateStorage.saveCertificates]
 * 6. Plaintext channel is closed — all subsequent communication uses mTLS via [NodeConnection]
 *
 * This runs **once per process start** in [de.polocloud.services.sdk.ServiceBoot].
 */
class ServiceRegistrationClient(
    private val storage: ServiceCertificateStorage,
) {

    private val logger = LoggerFactory.getLogger(ServiceRegistrationClient::class.java)

    /**
     * Registers this service instance with the node.
     *
     * @param registrationAddress plaintext registration endpoint of the node
     * @param serviceId           UUID of this service instance (from `-Dservice.id`)
     * @param token               single-use token from `-Dservice.token`
     * @throws IllegalStateException if registration is denied or the connection fails
     */
    fun register(registrationAddress: Address, serviceId: String, token: String) {
        logger.info("Registering service '{}' at '{}'", serviceId, registrationAddress)

        val channel = GrpcChannelFactory.plaintext(registrationAddress)

        try {
            val stub = ServiceRegistrationServiceGrpcKt
                .ServiceRegistrationServiceCoroutineStub(channel)

            val csrPem = CertificateSigningRequestGenerator(storage.keyPair, serviceId)
                .generate()
                .toPem()

            val response = runBlocking {
                stub.registerService(
                    RegisterServiceRequest.newBuilder()
                        .setServiceId(serviceId)
                        .setToken(token)
                        .setCsrPem(csrPem)
                        .build()
                )
            }

            check(response.accepted) {
                "Service registration denied by node: ${response.message}"
            }

            storage.saveCertificates(response.certificate, response.caCertificate)

            logger.info("Service '{}' registered — certificate received", serviceId)

        } finally {
            GrpcChannelFactory.shutdown(channel)
        }
    }
}