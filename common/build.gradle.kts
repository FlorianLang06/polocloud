plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.git.properties)

    id("dev.httpmarco.polocloud")
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.polocloud.i18n)

    implementation(libs.kotlinx.serialization.json)
}

gitProperties {
    extProperty = "gitProps"
    keys = listOf(
        "git.commit.id",
        "git.commit.id.abbrev"
    )
}

tasks.processResources {
    dependsOn(tasks.generateGitProperties)
}