plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.node.PolocloudNodeLauncher"
}

repositories {
    mavenLocal()
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
    compileOnly(libs.gson)
    compileOnly(libs.polocloud.i18n)

    runtimeOnly(libs.bundles.grpc)
    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
    runtimeOnly(libs.postgreSql)
    runtimeOnly(libs.gson)
    runtimeOnly(libs.polocloud.i18n)
}