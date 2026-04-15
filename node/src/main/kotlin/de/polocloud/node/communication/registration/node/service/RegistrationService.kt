package de.polocloud.node.communication.registration.node.service

import de.polocloud.common.communication.certificate.certToPem
import de.polocloud.common.communication.certificate.parseCsr
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeIndexGenerator
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.node.security.SanBuilder
import de.polocloud.proto.NodeRegistrationServiceGrpcKt
import de.polocloud.proto.NodeState
import de.polocloud.proto.RegisterNodeRequest
import de.polocloud.proto.RegisterNodeResponse
import org.slf4j.LoggerFactory
import java.util.*

class RegistrationService(
    val registrationManager: RegistrationManager,
) : NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(RegistrationService::class.java)

    override suspend fun registerNode(request: RegisterNodeRequest): RegisterNodeResponse {

        logger.trInfo("cluster", "cluster.registration.node.starting")

        if (NodeRepository.find(UUID.fromString(request.localId)) != null) {
            return this.sendDenyResponse("cluster.registration.node.alreadyRegistered")
        }

        if (!registrationManager.registrationTokenManger.validate(request.token)) {
            return this.sendDenyResponse("cluster.registration.node.token.invalid")
        }

        if (request.details.version != PolocloudVersion.CURRENT.toString()) {
            return this.sendDenyResponse("cluster.registration.node.version.mismatch")
        }

        val nodeIndex = NodeIndexGenerator.generate()
        val nodeName = "${request.group}-$nodeIndex"

        val ca = CertificateDataStorage.certificateAuthority()

        val csr = parseCsr(request.publicKey)
        val sans = SanBuilder.forNode(request.hostname, request.group, nodeIndex)
        val cert = ca.signCsr(csr, subjectAltNames = sans)
        val certPem = certToPem(cert)

        NodeRepository.save(
            NodeData(
                UUID.fromString(request.localId),
                nodeIndex,
                request.group,
                request.hostname,
                request.port,
                NodeState.STARTING,
                false,
                request.details.version,
                request.details.gitHash
            )
        )

        logger.trInfo("cluster", "cluster.registration.node.registered")
        return RegisterNodeResponse.newBuilder()
            .setCertificate(certPem)
            .setCaCertificate(ca.getCaCertificatePem())
            .setAccepted(true)
            .build()
    }

    fun sendDenyResponse(messageId: String): RegisterNodeResponse {
        logger.trInfo("cluster", messageId)
        return RegisterNodeResponse.newBuilder().setAccepted(false).setMessage(messageId).build()
    }
}