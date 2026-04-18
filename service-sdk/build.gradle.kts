import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.10"
    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.services.sdk.ServiceBootKt"
}

dependencies {
    compileOnly(projects.common)
    compileOnly(projects.proto)

    polocloudRuntime(libs.kotlinx.coroutines.core)

    // logging
    polocloudRuntime(libs.bundles.logging.full)
    polocloudRuntime(libs.bundles.grpc.node)
    polocloudRuntime(libs.bundles.tls)

    polocloudRuntime(libs.polocloud.database)
}