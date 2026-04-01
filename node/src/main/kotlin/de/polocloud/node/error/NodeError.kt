package de.polocloud.node.error

import kotlinx.serialization.Serializable
import de.polocloud.common.error.BasePoloError
import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.context.ErrorContext

sealed class NodeError {

    @Serializable
    data class DatabaseConnectionFailed(val url: String) : BasePoloError(
        code = NodeErrorCodes.DATABASE_CONNECTION_FAILED,
        key = "node.database.connection_failed", //"Cluster database connection is not valid."
        placeholders = mapOf("url" to url),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("NodeInstance.initializeDatabase", "url" to url),
    )

    @Serializable
    data class NotRegisteredInCluster(val nodeId: String) : BasePoloError(
        code = NodeErrorCodes.NOT_REGISTERED_IN_CLUSTER,
        key = "node.cluster.not_registered",
        placeholders = mapOf("nodeId" to nodeId),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("NodeInstance.initialize", "nodeId" to nodeId),
    )

    @Serializable
    data class RegistrationFailed(val address: String, val reason: String) : BasePoloError(
        code         = NodeErrorCodes.REGISTRATION_FAILED,
        key          = "node.registration.failed",
        placeholders = mapOf("address" to address, "reason" to reason),
        severity     = ErrorSeverity.CRITICAL,
        context      = ErrorContext.from("RegistrationClient.tryRegister", "address" to address),
    )

    @Serializable
    data class HeartbeatSaveFailed(val nodeId: String, val reason: String) : BasePoloError(
        code         = NodeErrorCodes.HEARTBEAT_SAVE_FAILED,
        key          = "node.heartbeat.save_failed",
        placeholders = mapOf("nodeId" to nodeId, "reason" to reason),
        severity     = ErrorSeverity.WARNING,
        context      = ErrorContext.from("NodeHeartBeatService.startScheduler", "nodeId" to nodeId),
    )

}