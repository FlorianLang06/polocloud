package de.polocloud.common.error

import de.polocloud.common.error.context.ErrorContext
import de.polocloud.common.error.exception.PoloException
import de.polocloud.common.error.exception.PoloResult
import de.polocloud.i18n.api.TranslationService

/**
 * Base for all structured errors in PoloCloud.
 *
 * Never throw this directly — use [asFailure] to wrap it in a [PoloResult],
 * or [throwIfFatal] to escalate only when severity demands it.
 *
 * Module-specific errors implement this in their own packages:
 * - `node/error/NodeError.kt`
 * - `cli/error/CliError.kt`
 */
interface PoloError {
    val code: ErrorCode

    val key: String
    val placeholders: Map<String, String>

    val severity: ErrorSeverity
    val context: ErrorContext

    val message: String
        get() = TranslationService.tr(
            pack = "errors",
            key = key,
            *placeholders.map { it.key to it.value }.toTypedArray()
        )

    /**
     * Wraps this error in a failed [PoloResult].
     */
    fun <T> asFailure(): PoloResult<T> = Result.failure(PoloException(this))

    /**
     * Throws a [PoloException] only if severity is [ErrorSeverity.FATAL].
     */
    fun throwIfFatal() {
        if (severity == ErrorSeverity.FATAL) throw PoloException(this)
    }

    /**
     * Human-readable representation for logging.
     */
    fun format(): String = "[${severity.name}] $code — $message (trace: ${context.traceId})"
}