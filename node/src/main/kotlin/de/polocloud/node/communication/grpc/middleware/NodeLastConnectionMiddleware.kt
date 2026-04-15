package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import de.polocloud.node.cluster.node.NodeRepository
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock

/**
 * Middleware that updates the lastConnection timestamp of a node
 * whenever a request is received from that node.
 *
 * Distinguishes nodes from CLI clients by checking if the certificate CN is a UUID:
 * - Nodes: CN=<UUID> (e.g. CN=550e8400-e29b-41d4-a716-446655440000)
 * - CLI: CN=<username> (e.g. CN=john.doe)
 */
class NodeLastConnectionMiddleware : GrpcServerMiddleware {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response {
        val clientIp = context.get<String>("clientIp")
        val subject = context.get<String>("subject")

        // Check if the subject is a UUID (indicates a node, not a CLI)
        if (subject != null && isUuid(subject)) {
            updateLastConnection(subject)
            logger.debug("Updated lastConnection for node $subject from $clientIp")
        }

        return next()
    }

    /**
     * Checks if the given string is a valid UUID.
     * This is used to distinguish node certificates (CN=UUID) from CLI certificates (CN=username).
     */
    private fun isUuid(value: String): Boolean {
        return runCatching { UUID.fromString(value) }.isSuccess
    }

    /**
     * Updates the lastConnection timestamp for the node with the given ID.
     */
    private fun updateLastConnection(nodeId: String) {
        runCatching {
            val uuid = UUID.fromString(nodeId)
            val node = NodeRepository.find(uuid)
            if (node != null) {
                node.lastConnection = Clock.System.now()
                NodeRepository.save(node)
            }
        }
    }
}
