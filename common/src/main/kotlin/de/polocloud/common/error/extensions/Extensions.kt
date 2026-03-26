package de.polocloud.common.error.extensions

import de.polocloud.common.error.PoloError
import de.polocloud.common.error.exception.PoloException
import de.polocloud.common.error.exception.PoloResult
import de.polocloud.common.error.reporting.ErrorReporter

/**
 * Reports the error to [ErrorReporter] if this result is a failure, then returns the result.
 */
fun <T> PoloResult<T>.reportIfFailure(): PoloResult<T> {
    ErrorReporter.reportFailure(this)
    return this
}

/**
 * Unwraps the value or reports the error and returns null.
 *
 * ```kotlin
 * val node = connectToCluster(info).getOrReport() ?: return
 * ```
 */
fun <T> PoloResult<T>.getOrReport(): T? {
    reportIfFailure()
    return getOrNull()
}

/**
 * Unwraps the value or reports the error and throws only if fatal.
 *
 * ```kotlin
 * val connection = openDatabase().getOrReportAndThrow()
 * ```
 */
fun <T> PoloResult<T>.getOrReportAndThrow(): T {
    reportIfFailure()
    return getOrThrow()
}

/**
 * Reports this error immediately and returns it for chaining.
 */
fun PoloError.report(): PoloError {
    ErrorReporter.report(this)
    return this
}

/**
 * Wraps this error in a failed [PoloResult] of the given type.
 */
fun <T> PoloError.asFailure(): PoloResult<T> = Result.failure(PoloException(this))

/**
 * Wraps a value in a successful [PoloResult].
 */
fun <T> T.asSuccess(): PoloResult<T> = Result.success(this)
