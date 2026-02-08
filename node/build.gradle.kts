plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.node.PolocloudNodeLauncher"
}

dependencies {
    compileOnly(projects.common)
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)

    compileOnly(libs.hikariCp)

    runtimeOnly(libs.bundles.grpc)
    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
}