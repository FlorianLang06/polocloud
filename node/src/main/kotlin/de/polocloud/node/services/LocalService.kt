package de.polocloud.node.services

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class LocalService(val service: Service) : Service(service.id, service.index, service.group, service.state) {

    var process: Process? = null
    var workDir: Path? = null

    /** Port the service was assigned and binds to; -1 until it has been started. */
    var port: Int = -1

    @OptIn(ExperimentalPathApi::class)
    fun shutdown() {
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

        // Give the OS a moment to release file handles before deleting the work
        // directory (Windows keeps the jar locked briefly after the process exits).
        if (!Thread.currentThread().isVirtual) {
            Thread.sleep(200)
        }

        // TODO: keep the work directory for static services once that flag exists.
        workDir?.deleteRecursively()
    }

}