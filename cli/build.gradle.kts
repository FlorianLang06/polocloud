import dev.httpmarco.polocloud.dependency.plugin.polocloudRuntime

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.cli.CliBootKt"
}

dependencies {
    polocloudRuntime("org.jline:jline:3.0.7")
    polocloudRuntime(libs.slf4j.api)
    polocloudRuntime(libs.log4j.api)
    polocloudRuntime(libs.log4j.core)
    polocloudRuntime(libs.log4j.slf4j)

    polocloudRuntime(libs.polocloud.i18n)
    polocloudRuntime(libs.kotlinx.serialization.json)

    compileOnly(projects.common)
}
