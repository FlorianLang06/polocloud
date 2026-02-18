plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}