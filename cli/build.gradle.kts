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
    polocloudRuntime(libs.jline)
    polocloudRuntime(libs.bundles.logging.full)
    polocloudRuntime(libs.bundles.polocloud.common)

    compileOnly(projects.common)
}
