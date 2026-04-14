package de.polocloud.node.bootstrap

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.ClasspathInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import de.polocloud.common.system.PolocloudSystemProperties
import java.nio.file.Path
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    val cliJar = Path.of(System.getProperty(PolocloudSystemProperties.RUNTIME_PATH)).toFile()

    dependencyRegistry.scan(OwnBlobScanner(cliJar))
    dependencyRegistry.downloadAndRegister()

    // try to clean the screen before starting the node
    println("\u001b[H\u001b[2J")

    // fallback exception handler
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("Fatal bootstrap error in thread '${thread.name}'")
        throwable.printStackTrace()
    }

    val launch = NodeLaunch(args).run()
    launch.start()

    // Block main thread until shutdown
    Thread.currentThread().join()
}