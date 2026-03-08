import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    id("de.polocloud")
}

polocloud {

}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(projects.common)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.bundles.database)
    compileOnly(libs.bundles.database.drivers)
    compileOnly(libs.bundles.polocloud.common)
    compileOnly(libs.gson)

    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.bundles.database)
    runtimeOnly(libs.bundles.database.drivers)
    runtimeOnly(libs.bundles.polocloud.common)
    runtimeOnly(libs.junit.platform.suite)

    testImplementation(projects.common)
    testImplementation(libs.bundles.logging)
    testImplementation(libs.bundles.database)
    testImplementation(libs.bundles.database.drivers)
    testImplementation(libs.bundles.polocloud.common)
    testImplementation(libs.gson)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.junit.platform.suite)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}