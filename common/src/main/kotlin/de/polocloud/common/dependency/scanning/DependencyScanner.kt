package de.polocloud.common.dependency.scanning

import de.polocloud.common.dependency.Dependency
import de.polocloud.common.dependency.DependencyBlob

interface DependencyScanner<T> {

    fun scanDependencies(): List<T>

    fun mapToDependency(dependency: T): Dependency

    fun doScanning(): DependencyBlob {
        return DependencyBlob(
            scanDependencies().map { mapToDependency(it) }
        )
    }
}