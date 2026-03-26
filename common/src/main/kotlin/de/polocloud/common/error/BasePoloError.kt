package de.polocloud.common.error

import de.polocloud.common.error.context.ErrorContext
import kotlinx.serialization.Serializable

/**
 * Convenience base class for [PoloError] implementations.
 * Use this instead of implementing the interface directly.
 *
 * ```kotlin
 * data class ClusterUnreachable(val address: Address) : BasePoloError(
 *     code      = NodeErrorCodes.CLUSTER_UNREACHABLE,
 *     message   = "Cannot reach cluster at $address",
 *     severity  = ErrorSeverity.CRITICAL,
 *     context   = ErrorContext.from("ClusterManager.connect", "address" to address.toString()),
 * )
 * ```
 */
@Serializable
abstract class BasePoloError(
    override val code: ErrorCode,
    override val key: String,
    override val placeholders: Map<String, String> = emptyMap(),
    override val severity: ErrorSeverity,
    override val context: ErrorContext,
) : PoloError