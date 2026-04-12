package de.polocloud.node.communication.cli.session

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.i18n.api.trError
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Periodically removes expired CLI sessions from the [ICliSessionManager].
 *
 * Extracted from [de.polocloud.node.communication.grpc.NodeGrpcEndpoint] to follow the single-responsibility principle.
 * The cleanup interval defaults to the same value as the session timeout.
 *
 * @param sessionManager  The session store to clean up
 * @param timeoutMs       How long a session may be inactive before it is considered expired
 * @param intervalMs      How often the cleanup runs (defaults to [timeoutMs])
 */
class CliSessionCleanup(
    private val sessionManager: ICliSessionManager,
    private val timeoutMs: Long = SESSION_TIMEOUT_MS,
    private val intervalMs: Long = timeoutMs,
) : Closeable {

    companion object {
        const val SESSION_TIMEOUT_MS = 60_000L
    }

    private val logger = LoggerFactory.getLogger(CliSessionCleanup::class.java)

    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "cli-session-cleanup").also { it.isDaemon = true }
    }

    fun start() {
        executor.scheduleAtFixedRate(
            ::runCleanup,
            intervalMs,
            intervalMs,
            TimeUnit.MILLISECONDS,
        )
    }

    override fun close(mode: ShutdownMode) {
        executor.shutdown()
    }

    private fun runCleanup() {
        runCatching {
            val before = sessionManager.all().size
            sessionManager.cleanupExpired(timeoutMs)
            val removed = before - sessionManager.all().size

            if (removed > 0) {
                logger.debug("Cleaned up $removed expired CLI session(s)")
            }
        }.onFailure { exception ->
            logger.trError("node", "node.session.cleanup_failed", exception)
        }
    }
}