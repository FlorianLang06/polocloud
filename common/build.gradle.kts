import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.git.properties)
    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.polocloud.i18n)

    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.bundles.tls)
    runtimeOnly(libs.bundles.tls)


    //database
    polocloudRuntime(libs.polocloud.database)
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