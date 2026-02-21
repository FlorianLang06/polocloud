package dev.httpmarco.polocloud.cli

import dev.httpmarco.polocloud.common.dependency.DependencyRegistry
import dev.httpmarco.polocloud.common.dependency.insert.ClasspathInsert
import dev.httpmarco.polocloud.common.dependency.scanning.OwnBlobScanner
import java.nio.file.Path

fun main() {
    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    // TODO
    val cliJar = Path.of(".cache/dev/httpmarco/polocloud/cli/3.0.0-pre.10-SNAPSHOT/cli-3.0.0-pre.10-SNAPSHOT.jar").toFile()

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