plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
    kotlin("plugin.serialization") version "1.9.10"
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.node.PolocloudNodeLauncher"


}

repositories {
    mavenCentral()
    maven {
        name = "polocloud-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    compileOnly(projects.common)
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.postgreSql)
    compileOnly(libs.hikariCp)
    compileOnly(libs.polocloud.i18n)
    compileOnly(projects.database)
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    runtimeOnly(libs.bundles.grpc)
    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
    runtimeOnly(libs.postgreSql)
    runtimeOnly(libs.polocloud.i18n)
    runtimeOnly(projects.database)
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}