plugins {
    kotlin("jvm") version "2.3.10"
    id("dev.httpmarco.polocloud")
    kotlin("plugin.serialization") version "2.3.10"
    alias(libs.plugins.gradle.git.properties)
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

gitProperties {
    extProperty = "gitProps"
    keys = listOf(
        "git.commit.id",
        "git.commit.id.abbrev"
    )
}


tasks.processResources {
    dependsOn(tasks.generateGitProperties)
}