package de.polocloud.node.registration

import de.polocloud.common.i18n.trInfo
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.proto.NodeRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterNodeRequest
import de.polocloud.proto.RegisterNodeResponse
import org.slf4j.LoggerFactory
import java.util.UUID

class RegistrationService(val registrationManager: RegistrationManager, val repository: NodeRepository) :
    NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(RegistrationService::class.java)

    override suspend fun registerNode(request: RegisterNodeRequest): RegisterNodeResponse {

        logger.trInfo("cluster", "cluster.registration.node.starting")

        if (repository.find(UUID.fromString(request.localId)) != null) {
            return this.sendDenyResponse("cluster.registration.node.alreadyRegistered")
        }

        if (!request.token.equals(registrationManager.publicRegistrationToken)) {
            return this.sendDenyResponse("cluster.registration.node.token.invalid")
        }

        if (request.details.version != PolocloudVersion.CURRENT.toString()) {
            return this.sendDenyResponse("cluster.registration.node.version.mismatch")
        }


        request.publicKey
        // hier neuen generieren

        logger.trInfo("cluster", "cluster.registration.node.registered")
        return RegisterNodeResponse.newBuilder().setAccepted(true).build()
    }

    fun sendDenyResponse(messageId: String): RegisterNodeResponse {
        logger.trInfo("cluster", messageId)
        return RegisterNodeResponse.newBuilder().setAccepted(false).setMessage(messageId).build()
    }
}