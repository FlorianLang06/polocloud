package de.polocloud.common.error.reporting

import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.PoloError
import de.polocloud.common.error.exception.PoloException
import de.polocloud.common.error.exception.PoloResult
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central error registry. All reported errors are stored here in memory.
 *
 * Designed to be the single source of truth for:
 * - Structured logging
 * - Status page feeds
 * - Monitoring / alerting hooks
 *
 * Register listeners via [onReport] to react to incoming errors globally.
 */
object ErrorReporter {

    private const val MAX_ENTRIES = 500

    private val logger = LoggerFactory.getLogger(ErrorReporter::class.java)

    private val _errors = CopyOnWriteArrayList<PoloError>()
    private val listeners = CopyOnWriteArrayList<(PoloError) -> Unit>()

    /**
     * Immutable snapshot of all recorded errors, newest first.
     */
    val errors: List<PoloError> get() = _errors.toList().asReversed()

    /**
     * Errors filtered by severity.
     */
    fun errorsOf(severity: ErrorSeverity): List<PoloError> = errors.filter { it.severity == severity }

    /**
     * Most recent N errors.
     */
    fun recent(limit: Int = 50): List<PoloError> = errors.take(limit)

    /**
     * Report an error. Stores it, notifies listeners, and logs it.
     * If severity is [ErrorSeverity.FATAL], throws a [de.polocloud.common.error.exception.PoloException] after reporting.
     */
    fun report(error: PoloError) {
        store(error)
        notifyListeners(error)
        log(error)
        error.throwIfFatal()
    }

    /**
     * Report from a failed [de.polocloud.common.error.exception.PoloResult] — no-op on success.
     */
    fun <T> reportFailure(result: PoloResult<T>) {
        result.exceptionOrNull()
            ?.let { it as? PoloException }
            ?.let { report(it.error) }
    }

    /**
     * Register a global listener that fires on every reported error.
     * Useful for forwarding to external monitoring, alerting, or status pages.
     *
     * ```kotlin
     * ErrorReporter.onReport { error ->
     *     if (error.severity == ErrorSeverity.CRITICAL) alertOpsTeam(error)
     * }
     * ```
     */
    fun onReport(listener: (PoloError) -> Unit) {
        listeners += listener
    }

    /**
     * Clears all stored errors. Useful in tests.
     */
    fun clear() = _errors.clear()

    private fun store(error: PoloError) {
        if (_errors.size >= MAX_ENTRIES) _errors.removeAt(0)
        _errors += error
    }

    private fun notifyListeners(error: PoloError) {
        listeners.forEach { listener ->
            runCatching { listener(error) }
                .onFailure { e -> System.err.println("[ErrorReporter] Listener threw: ${e.message}") }
        }
    }

    private fun log(error: PoloError) {
        val message = error.format()
        when (error.severity) {
            ErrorSeverity.FATAL -> logger.error(message)
            ErrorSeverity.CRITICAL -> logger.error(message)
            ErrorSeverity.WARNING -> logger.warn(message)
            ErrorSeverity.INFO -> logger.info(message)
        }
    }
}