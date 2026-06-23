package de.polocloud.node.services

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class LocalService(val service: Service) : Service(service.id, service.index, service.group, service.state) {

    var process: Process? = null
    var workDir: Path? = null

    @OptIn(ExperimentalPathApi::class)
    fun shutdown() {
        process?.let { process ->
            try {
                //  if (shutdownCommand.isNotEmpty() && shutdownCleanUp && service.executeCommand(shutdownCommand)) {
                // Wait a short time for graceful exit
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    service.state = ServiceState.STOPPED
                }
                //}
            } catch (_: Exception) {
                // Ignore exceptions, we only care about stopping the process
            }

            // Force-stop any remaining processes if still running
            if (service.state != ServiceState.STOPPED) {
                process.toHandle().children().forEach { child ->
                    try {
                        child.destroy()
                    } catch (_: Exception) { /* ignore */
                    }
                }
                process.toHandle().destroyForcibly()
                process.waitFor()
//                process = null
                service.state = ServiceState.STOPPED
            }
        }


        // Give Windows a small delay to ensure process termination
        if (!Thread.currentThread().isVirtual) {
            Thread.sleep(200)
        }

        // Delete service files if not static
        //  if (!service.isStatic()) {
        workDir?.deleteRecursively()
        //}
    }

}