package de.polocloud.cli.error

import de.polocloud.common.error.BasePoloError
import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.context.ErrorContext

sealed class CliError {

    data class RegistrationDenied(val reason: String) : BasePoloError(
        code = CliErrorCodes.REGISTRATION_DENIED,
        key = "cli.registration.denied",
        placeholders = mapOf("reason" to reason),
        severity = ErrorSeverity.WARNING,
        context = ErrorContext.from("ClusterConnection.register"),
    )

    data class RegistrationFailed(val address: String, val causeMsg: String) : BasePoloError(
        code = CliErrorCodes.REGISTRATION_FAILED,
        key = "cli.registration.failed",
        placeholders = mapOf("address" to address, "reason" to causeMsg),
        severity = ErrorSeverity.CRITICAL,
        context = ErrorContext.from("ClusterConnection.register", "address" to address),
    )
}