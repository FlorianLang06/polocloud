plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.node.PolocloudNodeLauncher"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "polocloud-snapshots"
    }
}

dependencies {

    // internal modules
    compileOnly(projects.common)
    compileOnly(projects.database)
    compileOnly(projects.proto)

    runtimeOnly(projects.common)
    runtimeOnly(projects.database)
    runtimeOnly(projects.proto)

    testImplementation(projects.common)
    testImplementation(projects.database)
    testImplementation(projects.proto)

    // grpc
    compileOnly(libs.bundles.grpc)
    runtimeOnly(libs.bundles.grpc)
    runtimeOnly(libs.bundles.grpc.runtime)

    // logging
    compileOnly(libs.bundles.logging)
    runtimeOnly(libs.bundles.logging)

    // database
    compileOnly(libs.bundles.database)
    runtimeOnly(libs.bundles.database)

    // kotlin
    compileOnly(libs.bundles.kotlin)
    runtimeOnly(libs.bundles.kotlin)

    // polocloud
    compileOnly(libs.polocloud.i18n)
    runtimeOnly(libs.polocloud.i18n)

    // cli / system
    compileOnly(libs.oshi)
    runtimeOnly(libs.oshi)

    // security
    compileOnly(libs.bundles.tls)
    runtimeOnly(libs.bundles.tls)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.bundles.junit)
    testImplementation(kotlin("test"))
    testImplementation("org.awaitility:awaitility:4.3.0")
}

tasks.test {
    useJUnitPlatform()
}