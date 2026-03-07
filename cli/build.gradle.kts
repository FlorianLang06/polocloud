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
    // i dont know why polocloudRuntime is not working here, so i have to add the dependencies manually
    compileOnly("org.jline:jline:4.0.0")
    compileOnly(libs.slf4j.api)
    compileOnly(libs.log4j.api)
    compileOnly(libs.log4j.core)
    compileOnly(libs.log4j.slf4j)

    compileOnly(libs.polocloud.i18n)
    compileOnly(libs.kotlinx.serialization.json)

    compileOnly(projects.common)
}
