import dev.httpmarco.polocloud.dependency.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.cli.CliBootKt"
}

dependencies {
    polocloudRuntime(libs.jline)
    polocloudRuntime(libs.slf4j.api)
    polocloudRuntime(libs.log4j.api)
    polocloudRuntime(libs.log4j.core)
    polocloudRuntime(libs.log4j.slf4j)

    polocloudRuntime(libs.polocloud.i18n)

    compileOnly(projects.common)
}
