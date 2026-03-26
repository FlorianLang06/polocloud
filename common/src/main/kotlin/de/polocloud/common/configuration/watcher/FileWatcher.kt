package de.polocloud.common.configuration.watcher

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import kotlin.concurrent.thread

/**
 * Watches a single file for modifications using Java NIO WatchService.
 * Runs on a daemon thread — no manual cleanup needed unless you call [stop].
 */
class FileWatcher(
    private val file: Path,
    private val onModified: () -> Unit
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchDir: Path = file.toAbsolutePath().parent ?: Path.of(".")

    @Volatile
    private var running = true

    init {
        watchDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
        startWatchThread()
    }

    private fun startWatchThread() {
        thread(isDaemon = true, name = "polocloud-config-watcher[${file.fileName}]") {
            while (running) {
                val key = runCatching { watchService.take() }.getOrNull() ?: break

                key.pollEvents()
                    .filter { it.kind() != StandardWatchEventKinds.OVERFLOW }
                    .map { it as WatchEvent<Path> }
                    .filter { it.context().fileName == file.fileName }
                    .forEach { _ ->
                        // Debounce: editors often fire multiple events on save
                        Thread.sleep(50)
                        onModified()
                    }

                if (!key.reset()) break
            }
        }
    }

    fun stop() {
        running = false
        runCatching { watchService.close() }
    }

}