package de.polocloud.common.error.exception

import de.polocloud.common.error.ErrorCode
import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.PoloError
import de.polocloud.common.error.context.ErrorContext

/**
 * The only exception type in PoloCloud.
 * Only thrown for [ErrorSeverity.FATAL] errors — everything else uses [PoloResult].
 *
 * Always wraps a [PoloError] so the full context is preserved.
 */
class PoloException(val error: PoloError) : Exception(error.format()) {

    val code: ErrorCode get() = error.code
    val severity: ErrorSeverity get() = error.severity
    val context: ErrorContext get() = error.context
}

/**
 * The primary return type for any operation that can fail.
 *
 * Success: `Result.success(value)`
 * Failure: `someError.asFailure()`
 */
typealias PoloResult<T> = Result<T>