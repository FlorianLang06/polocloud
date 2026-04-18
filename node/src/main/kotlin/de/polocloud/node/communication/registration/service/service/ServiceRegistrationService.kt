package de.polocloud.node.communication.registration.service.service

import de.polocloud.common.communication.certificate.certToPem
import de.polocloud.common.communication.certificate.parseCsr
import de.polocloud.node.communication.registration.service.token.ServiceRegistrationTokenManager
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.security.SanBuilder
import de.polocloud.node.services.process.ServiceProcessRepository
import de.polocloud.proto.RegisterServiceRequest
import de.polocloud.proto.RegisterServiceResponse
import de.polocloud.proto.ServiceRegistrationServiceGrpcKt
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * gRPC service that handles **service instance registration**.
 *
 * Hosted on the plaintext registration port — identical lifecycle to
 * [de.polocloud.node.communication.registration.node.service.RegistrationService]
 * for nodes and [de.polocloud.node.communication.registration.cli.CliRegistrationService]
 * for CLI clients.
 *
 * Security guarantees:
 * - Token is short-lived (60 s) and single-use
 * - Token is bound to the exact [serviceId] → a leaked token cannot register a different service
 * - Only services whose [ServiceProcess] already exists in the DB are accepted
 *   (the node creates the process entry *before* booting the JVM)
 */
class ServiceRegistrationService(
    private val tokenManager: ServiceRegistrationTokenManager,
) : ServiceRegistrationServiceGrpcKt.ServiceRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(ServiceRegistrationService::class.java)

    override suspend fun registerService(request: RegisterServiceRequest): RegisterServiceResponse {
        val serviceId = runCatching { UUID.fromString(request.serviceId) }.getOrElse {
            return deny("Invalid service ID format: '${request.serviceId}'")
        }

        // 1. Validate the single-use token (also checks serviceId binding)
        if (!tokenManager.validate(request.token, serviceId)) {
            logger.warn("Service registration denied — invalid or expired token for service '{}'", serviceId)
            return deny("Invalid or expired registration token")
        }

        // 2. Verify the ServiceProcess exists (created by ServiceFactory before boot)
        val process = ServiceProcessRepository.find(serviceId)
            ?: return deny("No ServiceProcess found for id '$serviceId'").also {
                logger.warn("Service registration denied — unknown service process '{}'", serviceId)
            }

        // 3. Parse and sign the CSR
        val csr = runCatching { parseCsr(request.csrPem) }.getOrElse {
            logger.warn("Service registration denied — invalid CSR for service '{}'", serviceId)
            return deny("Invalid CSR")
        }

        val ca = NodeCertificateStorage.certificateAuthority()
        val sans = SanBuilder.forService(serviceId.toString(), process.plan)
        val cert = ca.signCsr(csr, validityDays = 1, subjectAltNames = sans)

        logger.info(
            "Service '{}' (plan='{}') registered successfully",
            serviceId,
            process.plan,
        )

        return RegisterServiceResponse.newBuilder()
            .setAccepted(true)
            .setCertificate(certToPem(cert))
            .setCaCertificate(ca.getCaCertificatePem())
            .build()
    }

    private fun deny(reason: String): RegisterServiceResponse =
        RegisterServiceResponse.newBuilder()
            .setAccepted(false)
            .setMessage(reason)
            .build()
}