package de.polocloud.node.launch

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.ClasspathInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import java.nio.file.Path

fun main(args: Array<String>) {
    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    // TODO
    //val version = PolocloudVersion.CURRENT.toVersionString() // TODO we cant do this here because error system ist not loaded because of dependencies
    val cliJar = Path.of(".cache/dependencies/de/polocloud/node/3.0.0-snapshot.local/node-3.0.0-snapshot.local.jar").toFile()

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