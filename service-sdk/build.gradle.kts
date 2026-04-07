import de.polocloud.dependency.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.10"
    id("de.polocloud")
}

dependencies {
    compileOnly(projects.common)
}