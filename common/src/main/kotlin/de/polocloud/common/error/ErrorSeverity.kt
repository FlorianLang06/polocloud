package de.polocloud.common.error

/**
 * Defines how critical an error is and how the system should react to it.
 *
 * - [FATAL]    → unrecoverable, triggers shutdown
 * - [CRITICAL] → node/service non-functional, no shutdown
 * - [WARNING]  → degraded behavior, retry possible
 * - [INFO]     → expected failure, fully recoverable
 */
enum class ErrorSeverity(val httpStatus: Int) {
    FATAL(500),
    CRITICAL(503),
    WARNING(400),
    INFO(404),
}