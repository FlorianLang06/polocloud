import de.polocloud.dependency.plugin.polocloudRuntime

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    id("de.polocloud")
}

polocloud {
    mainClass = "de.polocloud.cli.CliBootKt"
}

dependencies {
    polocloudRuntime(libs.jline)
    polocloudRuntime(libs.slf4j.api)
    polocloudRuntime(libs.log4j.api)
    polocloudRuntime(libs.log4j.core)
    polocloudRuntime(libs.log4j.slf4j)

    polocloudRuntime(libs.polocloud.i18n)
    polocloudRuntime(libs.kotlinx.serialization.json)
    polocloudRuntime(libs.kotlinx.coroutines.core)
    polocloudRuntime(libs.kotlin.reflect)

    compileOnly(projects.common)
}
