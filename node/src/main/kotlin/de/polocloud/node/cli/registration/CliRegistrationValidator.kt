package de.polocloud.node.cli.registration

import de.polocloud.node.configuration.cluster.CliAccessConfiguration

/**
 * Validates incoming CLI registration requests against the cluster configuration.
 *
 * Extracted from [CliRegistrationService] to isolate validation logic
 * and make it independently testable without a full gRPC context.
 */
class CliRegistrationValidator(private val config: CliAccessConfiguration) {

    sealed interface Result {
        data object Ok : Result
        data class Denied(val translationKey: String) : Result
    }

    fun validateAccess(): Result {
        if (!config.enabled) return Result.Denied("cli.access.disabled")
        return Result.Ok
    }

    fun validateToken(token: String): Result {
        if (token != config.registrationToken) return Result.Denied("cluster.registration.cli.token.invalid")
        return Result.Ok
    }
}