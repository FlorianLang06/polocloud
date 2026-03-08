plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    id("de.polocloud")
}

polocloud {
    mainClass = "de.polocloud.node.PolocloudNodeLauncher"
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
    compileOnly(libs.bundles.logging.full)
    runtimeOnly(libs.bundles.logging.full)

    // database
    compileOnly(libs.bundles.database)
    runtimeOnly(libs.bundles.database)

    // kotlin
    compileOnly(libs.bundles.kotlin.full)
    runtimeOnly(libs.bundles.kotlin.full)

    // polocloud
    compileOnly(libs.polocloud.i18n)
    runtimeOnly(libs.polocloud.i18n)

    // cli / system
    compileOnly(libs.oshi)
    runtimeOnly(libs.oshi)

    // security
    compileOnly(libs.bundles.tls)
    runtimeOnly(libs.bundles.tls)

    // hashing
    compileOnly(libs.argon2)
    runtimeOnly(libs.argon2)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.bundles.testing)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}