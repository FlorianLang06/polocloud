package dev.httpmarco.polocloud.node.registration

import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.node.NodeIndexGenerator
import dev.httpmarco.polocloud.proto.NodeRegistrationServiceGrpcKt
import dev.httpmarco.polocloud.proto.RegisterNodeRequest
import dev.httpmarco.polocloud.proto.RegisterNodeResponse
import org.slf4j.LoggerFactory
import java.util.UUID

class RegistrationService(val tokenStore: RegistrationTokenStore, val nodeRepository: dev.httpmarco.polocloud.node.repository.NodeRepository) :
    NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(RegistrationService::class.java)

    override suspend fun registerNode(request: RegisterNodeRequest): RegisterNodeResponse {
        val nodeId = parseUuid(request.localId)
            ?: return errorResponse(
                "cluster.registration.invalidId",
                "cluster.registration.invalidId.response",
                "nodeId" to request.localId
            )

        if (!tokenStore.verify(request.token)) {
            return errorResponse(
                "cluster.registration.token.invalid",
                "cluster.registration.token.invalid.response",
                "nodeId" to request.localId
            )
        }

        if (nodeRepository.findNode(nodeId) != null) {
            return errorResponse(
                "cluster.registration.duplicateId",
                "cluster.registration.duplicateId.response",
                "nodeId" to request.localId
            )
        }


        // TODO compare version - RecherGG

        tokenStore.rotate()

        val data = _root_ide_package_.dev.httpmarco.polocloud.node.node.data.NodeData(
            id = nodeId,
            index = NodeIndexGenerator.findNextFreeIndex(nodeRepository),
            port = request.port,
            hostname = request.hostname,
            // New nodes start as OFFLINE until they complete the full registration process
            state = _root_ide_package_.dev.httpmarco.polocloud.node.node.NodeState.OFFLINE,
            head = false,
            publicKey = request.publicKey,
            version = request.details.version,
            gitCommitHash = request.details.gitHash
        );

        nodeRepository.save(data)
        logger.info(TranslationService.tr("cluster", "cluster.registration.success", "nodeId" to data.name()))

        return RegisterNodeResponse.newBuilder()
            .setAccepted(true)
            .setMessage(
                TranslationService.tr(
                    "cluster",
                    "cluster.registration.success.external",
                    "nodeId" to data.name()
                )
            )
            .build()
    }

    private fun parseUuid(value: String): UUID? {
        return try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun errorResponse(
        logKey: String,
        responseKey: String,
        vararg args: Pair<String, String>
    ): RegisterNodeResponse {

        logger.debug(TranslationService.tr("cluster", logKey, *args))

        return RegisterNodeResponse.newBuilder()
            .setAccepted(false)
            .setMessage(TranslationService.tr("cluster", responseKey, *args))
            .build()
    }
}