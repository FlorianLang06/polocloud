package de.polocloud.node.services

import de.polocloud.shared.service.ServiceState
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class LocalService(private val service: Service) : Service(
    service.id, service.index, service.groupName, service.state, service.hostname, service.port
) {

    private companion object {
        val logger = LoggerFactory.getLogger(LocalService::class.java)

        /** How many recent log lines are retained per service for the `logs` tail. */
        const val LOG_BUFFER_CAPACITY = 300
    }

    var process: Process? = null
    var workDir: Path? = null

    /**
     * Whether this service belongs to a static group. Static services keep their work
     * directory (world/config) across restarts instead of being wiped on shutdown.
     */
    var static: Boolean = false

    /**
     * Free-form key/value properties, seeded from the owning group when the service
     * starts. Kept in-memory only (not a persisted [Service] column) — services are
     * ephemeral and re-seed their properties from the group on every start.
     */
    val properties: MutableMap<String, String> = mutableMapOf()

    /**
     * Players currently connected / configured player slots, as last reported by
     * [de.polocloud.node.services.ping.ServicePingFactory] over the Minecraft Server List
     * Ping. `0` until the first successful ping. These are only ever written by the ping
     * loop — nothing else in the node should assign them, keeping the value a read-only
     * reflection of what the service itself reports.
     */
    var onlinePlayers: Int = 0
    var maxPlayers: Int = 0

    /** Millis timestamp of the last player-count ping; used to throttle [de.polocloud.node.services.ping.ServicePingFactory] polling of already-running services. */
    var lastPlayerPollAt: Long = 0

    // Ring buffer of recent stdout/stderr lines (the process is started with
    // redirectErrorStream=true, so both arrive on the same stream).
    private val logBuffer = ArrayDeque<String>(LOG_BUFFER_CAPACITY)

    // Live consumers (e.g. an open `service <name> logs` stream). CopyOnWrite so the
    // reader thread can iterate while a CLI stream subscribes/unsubscribes concurrently.
    private val logListeners = CopyOnWriteArrayList<(String) -> Unit>()

    private var logReader: Thread? = null

    /**
     * Starts a daemon thread that pumps the process output into [logBuffer] and to
     * every registered [logListeners] entry. Call once, right after the process starts.
     */
    fun startLogCapture() {
        val proc = process ?: return
        val reader = Thread({
            runCatching {
                proc.inputStream.bufferedReader().use { buffered: BufferedReader ->
                    buffered.forEachLine { line ->
                        appendLog(line)
                    }
                }
            }
        }, "service-log-${name()}").apply { isDaemon = true }
        logReader = reader
        reader.start()
    }

    private fun appendLog(line: String) {
        synchronized(logBuffer) {
            if (logBuffer.size >= LOG_BUFFER_CAPACITY) logBuffer.removeFirst()
            logBuffer.addLast(line)
        }
        // Isolate listeners: a slow/failing consumer must not stall log capture.
        logListeners.forEach { listener -> runCatching { listener(line) } }
    }

    /** A snapshot of the currently buffered recent log lines. */
    fun recentLogs(): List<String> = synchronized(logBuffer) { logBuffer.toList() }

    fun addLogListener(listener: (String) -> Unit) {
        logListeners += listener
    }

    fun removeLogListener(listener: (String) -> Unit) {
        logListeners -= listener
    }

    /**
     * Writes [command] to the process's stdin (followed by a newline) so the service
     * executes it in its own console. Returns `false` if the process is not running.
     */
    fun executeCommand(command: String): Boolean {
        val proc = process ?: return false
        if (!proc.isAlive) return false
        return runCatching {
            val stdin = proc.outputStream
            stdin.write((command + System.lineSeparator()).toByteArray())
            stdin.flush()
            true
        }.getOrElse {
            logger.warn("Failed to write command to {}: {}", name(), it.message)
            false
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun shutdown() {
        logListeners.clear()
        process?.let { process ->
            val handle = process.toHandle()
            // Capture the full process tree up front: once the root exits its
            // descendants can no longer be enumerated, and Windows does not
            // cascade termination — leftover children would be orphaned.
            val tree = handle.descendants().toList() + handle

            // Ask the whole tree to terminate gracefully, then give it a moment.
            tree.forEach { runCatching { it.destroy() } }
            runCatching { process.waitFor(5, TimeUnit.SECONDS) }

            // Force-kill anything that ignored the graceful request.
            tree.filter { it.isAlive }.forEach { runCatching { it.destroyForcibly() } }
            runCatching { process.waitFor(2, TimeUnit.SECONDS) }

            service.state = ServiceState.STOPPED
        }

        // Best-effort: a failing repository delete must not skip the work-directory cleanup below.
        runCatching { ServiceRepository.delete(service) }

        // Give the OS a moment to release file handles before deleting the work
        // directory (Windows keeps the jar locked briefly after the process exits).
        if (!Thread.currentThread().isVirtual) {
            Thread.sleep(200)
        }

        // Static services keep their work directory (persistent world/config); only
        // ephemeral services get their directory wiped on shutdown.
        if (!static) {
            workDir?.deleteRecursively()
        }
    }

}
