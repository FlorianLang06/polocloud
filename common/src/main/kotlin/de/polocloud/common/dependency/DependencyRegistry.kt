package de.polocloud.common.dependency

import de.polocloud.common.dependency.insert.DependencyInsert
import de.polocloud.common.dependency.scanning.DependencyScanner

/**
 * Manages a collection of dependencies, allowing scanning, downloading, and binding
 * them for runtime use.
 *
 * @property insert the [DependencyInsert] responsible for rendering or injecting dependencies
 */
class DependencyRegistry(val insert: DependencyInsert<*>) {

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
     * Downloads all registered dependencies in parallel.
     *
     * This uses [parallelStream] to improve download performance.
     */
    fun downloadAndRegister() {
        registeredDependencies.forEach { //TODO add parallelStream but set the right classloader
            it.download()
            this.insert.register(it)
        }
    }

    /**
     * Binds or renders the dependencies using the configured [DependencyInsert].
     *
     * Currently private; can be used internally after scanning or downloading dependencies.
     */
}
