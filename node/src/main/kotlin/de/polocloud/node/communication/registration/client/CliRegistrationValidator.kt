package de.polocloud.node.communication.registration.client

import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.core.configuration.NodeConfigurations

/**
 * Validates incoming CLI registration requests against the cluster configuration.
 *
 * Extracted from [de.polocloud.node.communication.registration.cli.CliRegistrationService] to isolate validation logic
 * and make it independently testable without a full gRPC context.
 */
class CliRegistrationValidator(
    val holder: ConfigurationHolder<NodeConfigurations>
) {

    sealed interface Result {
        data object Ok : Result
        data class Denied(val translationKey: String) : Result
    }

    fun validateAccess(): Result {
        if (!holder.value.cluster.cliAccess.enabled) return Result.Denied("cli.access.disabled")
        return Result.Ok
    }

    fun validateToken(token: String): Result {
        if (token != holder.value.cluster.cliAccess.registrationToken) return Result.Denied("cluster.registration.cli.token.invalid")
        return Result.Ok
    }
}