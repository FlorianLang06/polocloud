import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.10"
    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    compileOnly(projects.common)
    polocloudRuntime(libs.polocloud.database)
}