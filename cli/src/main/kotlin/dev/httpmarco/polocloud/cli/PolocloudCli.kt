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

}