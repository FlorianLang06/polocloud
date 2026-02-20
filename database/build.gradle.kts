import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
    kotlin("plugin.serialization") version "1.9.10"
}

polocloud {

}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(libs.bundles.logging)
    compileOnly(projects.common)
    compileOnly(libs.postgreSql)
    compileOnly(libs.hikariCp)
    compileOnly(libs.gson)
    compileOnly(libs.polocloud.i18n)
    compileOnly(libs.bundles.database.drivers)
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.10")

    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
    runtimeOnly(libs.postgreSql)
    runtimeOnly(libs.polocloud.i18n)
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
    runtimeOnly("org.junit.platform:junit-platform-suite:1.10.0")

    runtimeOnly(libs.bundles.database.drivers)

    testImplementation(libs.gson)
    testImplementation(projects.common)
    testImplementation(libs.bundles.logging)
    testImplementation(libs.hikariCp)
    testImplementation(libs.postgreSql)
    testImplementation(libs.polocloud.i18n)
    testImplementation(kotlin("test"))


    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.bundles.database.drivers)

    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}