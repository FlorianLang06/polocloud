import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.node.bootstrap.NodeLaunchBootstrapKt"
}

dependencies {
    //kotlin
    polocloudRuntime(libs.kotlinx.serialization.json)
    polocloudRuntime(libs.kotlinx.coroutines.core)
    polocloudRuntime(libs.kotlin.reflect)

    //logging
    kapt(libs.log4j.core)
    polocloudRuntime(libs.bundles.logging.full)

    // grpc
    polocloudRuntime(libs.bundles.grpc.node)

    //polocloud
    polocloudRuntime(libs.polocloud.i18n)

    //hashing
    polocloudRuntime(libs.argon2)

    //security
    polocloudRuntime(libs.bundles.tls)

    //system
    polocloudRuntime(libs.oshi)

    //database
    polocloudRuntime(libs.polocloud.database)

    // cli
    polocloudRuntime(libs.jline)

    compileOnly(projects.common)
    implementation(projects.proto)
    implementation(projects.api)

    // testing
    testImplementation(projects.common)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("PID", ProcessHandle.current().pid().toString())
    useJUnitPlatform()
}