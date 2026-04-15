package de.polocloud.common.dependency

import de.polocloud.common.dependency.insert.DependencyInsert
import de.polocloud.common.dependency.scanning.DependencyScanner
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
     */
    fun downloadAndRegister() {
        if (registeredDependencies.isEmpty()) return

        val executor = Executors.newFixedThreadPool(registeredDependencies.size.coerceAtMost(8))

        try {
            val futures = registeredDependencies.map { dependency ->
                executor.submit(Callable {
                    dependency.download()
                    dependency
                })
            }

            futures.forEach { future ->
                val dependency = future.get()
                insert.register(dependency)
            }
            println("All ${registeredDependencies.size} dependencies resolved.")
        } finally {
            executor.shutdown()
        }
    }

    fun collect(): List<T> {
        return registeredDependencies.map { insert.renderDependency(it) }
    }

    /**
     * Downloads all registered dependencies in parallel into the global cache without
     * injecting them into the current runtime. Paths are collected separately via [collect].
     */
    fun downloadAll() {
        if (registeredDependencies.isEmpty()) return

        val executor = Executors.newFixedThreadPool(registeredDependencies.size.coerceAtMost(8))

        try {
            val futures = registeredDependencies.map { dependency ->
                executor.submit { dependency.download() }
            }
            futures.forEach { it.get() }
            println("All ${registeredDependencies.size} dependencies resolved.")
        } finally {
            executor.shutdown()
        }
    }
}
