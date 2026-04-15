package de.polocloud.common.dependency

import de.polocloud.common.dependency.insert.DependencyInsert
import de.polocloud.common.dependency.scanning.DependencyScanner
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Manages a collection of dependencies, allowing scanning, downloading, and binding
 * them for runtime use.
 *
 * @property insert the [DependencyInsert] responsible for rendering or injecting dependencies
 */
class DependencyRegistry<T>(val insert: DependencyInsert<T>) {

    /** Internal list of registered dependencies */
    private val registeredDependencies = mutableListOf<Dependency>()

    /**
     * Scans for dependencies using the provided [DependencyScanner] and registers them.
     *
     * @param scanner the scanner used to find dependencies
     */
    fun scan(scanner: DependencyScanner<*>) {
        this.registeredDependencies.addAll(scanner.doScanning().blobEntries)
    }

    /**
     * Downloads all registered dependencies in parallel and registers them via [insert].
     * If a downloaded JAR itself contains a [dependencies.index][OwnBlobScanner], those
     * transitive dependencies are also downloaded and inserted.
     */
    fun downloadAndRegister() {
        if (registeredDependencies.isEmpty()) return

        val processed = mutableSetOf<String>()
        val seen = registeredDependencies.mapTo(mutableSetOf()) { it.key() }
        val queue = ArrayDeque(registeredDependencies)

        while (queue.isNotEmpty()) {
            val batch = drainUnprocessed(queue, processed)
            if (batch.isEmpty()) break

            val executor = Executors.newFixedThreadPool(batch.size.coerceAtMost(8))
            try {
                val futures = batch.map { dependency ->
                    executor.submit(Callable {
                        dependency.download()
                        dependency
                    })
                }
                futures.forEach { future ->
                    val dependency = future.get()
                    insert.register(dependency)
                    enqueueNested(dependency, queue, seen, registeredDependencies)
                }
            } finally {
                executor.shutdown()
            }
        }
    }

    fun collect(): List<T> {
        return registeredDependencies.map { insert.renderDependency(it) }
    }

    /**
     * Downloads all registered dependencies in parallel into the global cache without
     * injecting them into the current runtime. Paths are collected separately via [collect].
     * If a downloaded JAR itself contains a [dependencies.index][OwnBlobScanner], those
     * transitive dependencies are also downloaded.
     */
    fun downloadAll() {
        if (registeredDependencies.isEmpty()) return

        val processed = mutableSetOf<String>()
        val seen = registeredDependencies.mapTo(mutableSetOf()) { it.key() }
        val queue = ArrayDeque(registeredDependencies)

        while (queue.isNotEmpty()) {
            val batch = drainUnprocessed(queue, processed)
            if (batch.isEmpty()) break

            val executor = Executors.newFixedThreadPool(batch.size.coerceAtMost(8))
            try {
                val futures = batch.map { dependency ->
                    executor.submit(Callable {
                        dependency.download()
                        dependency
                    })
                }
                futures.forEach { future ->
                    val dependency = future.get()
                    enqueueNested(dependency, queue, seen, registeredDependencies)
                }
            } finally {
                executor.shutdown()
            }
        }
    }

    /**
     * Drains all items from [queue] that have not yet been processed, marking each as processed.
     */
    private fun drainUnprocessed(queue: ArrayDeque<Dependency>, processed: MutableSet<String>): List<Dependency> {
        val batch = mutableListOf<Dependency>()
        while (queue.isNotEmpty()) {
            val dep = queue.removeFirst()
            if (processed.add(dep.key())) {
                batch.add(dep)
            }
        }
        return batch
    }

    /**
     * Checks whether [dep]'s downloaded JAR contains a `dependencies.index` and, if so,
     * enqueues any previously-unseen entries for download in the next batch.
     */
    private fun enqueueNested(
        dep: Dependency,
        queue: ArrayDeque<Dependency>,
        seen: MutableSet<String>,
        all: MutableList<Dependency>
    ) {
        val localFile = dep.localPath().toFile()
        if (!localFile.exists()) return
        try {
            OwnBlobScanner(localFile).doScanning().blobEntries.forEach { nested ->
                if (seen.add(nested.key())) {
                    queue.add(nested)
                    all.add(nested)
                }
            }
        } catch (_: Exception) {
            // No dependencies.index present in this JAR — expected for most dependencies
        }
    }

    private fun Dependency.key() = "$groupId:$artifactId:$version"
}
