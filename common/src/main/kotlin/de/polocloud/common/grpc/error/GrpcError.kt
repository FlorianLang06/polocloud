package de.polocloud.common.grpc.error

import de.polocloud.common.error.BasePoloError
import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.context.ErrorContext
import kotlinx.serialization.Serializable

sealed class GrpcError {

    @Serializable
    data class BindFailed(val address: String): BasePoloError(
        code = GrpcErrorCodes.BIND_FAILED,
        key = "grpc.bind_failed",
        placeholders = mapOf("address" to address),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("GrpcEndpoint.start", "address" to address)
    )

    @Serializable
    data class TlsSetupFailed(val reason: String) : BasePoloError(
        code = GrpcErrorCodes.TLS_SETUP_FAILED,
        key = "grpc.tls_setup_failed",
        placeholders = mapOf("reason" to reason),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("GrpcEndpoint.start"),
    )

    @Serializable
    data class ShutdownTimeout(val timeoutSeconds: Long) : BasePoloError(
        code = GrpcErrorCodes.SHUTDOWN_TIMEOUT,
        key = "grpc.shutdown_timeout",
        placeholders = mapOf("timeout" to timeoutSeconds.toString()),
        severity = ErrorSeverity.WARNING,
        context = ErrorContext.from("GrpcEndpoint.close"),
    )
}