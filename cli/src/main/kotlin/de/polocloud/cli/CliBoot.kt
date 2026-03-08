package de.polocloud.cli

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.ClasspathInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import de.polocloud.common.version.PolocloudVersion
import java.nio.file.Path

fun main() {
    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    // TODO
    val version = PolocloudVersion.CURRENT.toVersionString()
    val cliJar = Path.of(".cache/dev/httpmarco/polocloud/cli/$version/cli-$version.jar").toFile()

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

    PolocloudCli.start()
    Thread.currentThread().join()
}