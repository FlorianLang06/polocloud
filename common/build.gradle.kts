plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.gson)
    compileOnly(libs.bundles.logging)
}