import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.node.launch.NodeLaunchBootstrapKt"
}

dependencies {
    //kotlin
    polocloudRuntime(libs.kotlinx.serialization.json)
    polocloudRuntime(libs.kotlinx.coroutines.core)
    polocloudRuntime(libs.kotlin.reflect)

    //logging
    kapt(libs.log4j.core)
    polocloudRuntime(libs.slf4j.api)
    polocloudRuntime(libs.log4j.api)
    polocloudRuntime(libs.log4j.core)
    polocloudRuntime(libs.log4j.slf4j)

    // grpc
    polocloudRuntime(libs.grpc.api)
    polocloudRuntime(libs.grpc.stub)
    polocloudRuntime(libs.grpc.kotlin.stub)
    polocloudRuntime(libs.grpc.services)
    polocloudRuntime(libs.protobuf.kotlin)
    polocloudRuntime(libs.grpc.netty.shaded)
    //"grpc-api", "grpc-stub", "grpc-services", "protobuf-java", "grpc-netty-shaded" old, maybe missing

    // database
    polocloudRuntime(libs.hikariCp)
    polocloudRuntime(libs.postgreSql)

    //polocloud
    polocloudRuntime(libs.polocloud.i18n)

    //hashing
    polocloudRuntime(libs.argon2)

    //security
    polocloudRuntime(libs.bcprov)
    polocloudRuntime(libs.bcpkix)

    //system
    polocloudRuntime(libs.oshi)

    compileOnly(projects.database)
    compileOnly(projects.common)
    compileOnly(projects.serviceSdk)
    implementation(projects.proto)
}

tasks.test {
    systemProperty("PID", ProcessHandle.current().pid().toString())
    useJUnitPlatform()
}