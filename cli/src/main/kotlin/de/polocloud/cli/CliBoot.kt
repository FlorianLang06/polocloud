package de.polocloud.cli

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.ClasspathInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import de.polocloud.common.version.PolocloudVersion
import java.nio.file.Path

fun main() {
    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    // TODO
    //val version = PolocloudVersion.CURRENT.toVersionString() // TODO we cant do this here because error system ist not loaded because of dependencies
    val cliJar = Path.of(".cache/dependencies/de/polocloud/cli/3.0.0-snapshot.local/cli-3.0.0-snapshot.local.jar").toFile()

    dependencyRegistry.scan(OwnBlobScanner(cliJar))
    dependencyRegistry.downloadAndRegister()

    // try to clean the screen before starting the agent
    println("\u001b[H\u001b[2J")

    // save boot time
    System.setProperty("polocloud-cli.lifecycle.boot-time", System.currentTimeMillis().toString())

    // register a clean hook for good shutdown
    registerHook()

    // fallback exception handler
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("Fatal bootstrap error in thread '${thread.name}'")
        throwable.printStackTrace()
    }

    Cli.start()
    Thread.currentThread().join()
}