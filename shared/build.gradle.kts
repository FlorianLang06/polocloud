plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    // Event payloads are @Serializable and (de)serialized via the shared EventCodec.
    // Exposed so consumers (api, node) get the serialization runtime transitively.
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
