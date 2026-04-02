package de.polocloud.node.shutdown

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.i18n.trError
import de.polocloud.common.i18n.trInfo
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Central manager for handling shutdown tasks in the Node.
 *
 * You can attach any component that implements [Closeable] to this manager.
 * When the JVM shuts down or when [shutdown] is called, all registered
 * tasks are executed in the order they were added.
 */
object ShutdownHook {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Thread-safe queue for shutdown tasks
    private val tasks: ConcurrentLinkedQueue<Closeable> = ConcurrentLinkedQueue()

    /**
     * Registers a [Closeable] task to be executed on shutdown.
     *
     * Thread-safe: multiple components can register simultaneously.
     *
     * @param task the component to close during shutdown
     */
    @Synchronized
    fun attach(task: Closeable) {
        tasks.add(task)
    }

    /**
     * Executes all registered shutdown tasks.
     *
     * @param mode the [ShutdownMode] to pass to each task (default: GRACEFUL)
     */
    fun shutdown(mode: ShutdownMode = ShutdownMode.GRACEFUL) {
        logger.trInfo("node", "node.shutdown.stopping")
        // Copy tasks to avoid concurrent modification if attach() is called during shutdown
        val tasksCopy: List<Closeable>
        synchronized(this) {
            tasksCopy = tasks.toList()
        }
        for (closeable in tasksCopy) {
            try {
                closeable.close(mode)
            } catch (ex: Exception) {
                logger.trError("node", "node.shutdown.task.error", ex, "task" to closeable.javaClass.name)
            }
        }
        logger.trInfo("node", "node.shutdown.stopped")
    }

    /**
     * Registers a JVM shutdown hook so that [shutdown] is automatically
     * called when the JVM terminates.
     *
     * This should typically be called once during node initialization.
     */
    fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown(ShutdownMode.GRACEFUL)
        })
    }
}