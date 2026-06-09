plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.polocloud.i18n)

    implementation(libs.kotlinx.serialization.json)
}
