plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    // exposed to SDK consumers: proto stubs/messages, common (Address, mTLS, channel factory)
    api(projects.proto)
    api(projects.common)

    // BouncyCastle is required at runtime by the inherited CertificateStorage
    implementation(libs.bundles.tls)

    // testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
