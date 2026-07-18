plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "de.polocloud"
    // version is now set by gradle/version.gradle.kts — do NOT set it here

    repositories {
        mavenCentral()

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

/**
 * Project-wide test suite: runs the `test` task of every module in one go.
 *
 * Usage: `./gradlew allTests`. This is the cross-module entry point — JUnit's own
 * `@Suite` only aggregates tests within a single module's classpath, so Gradle is
 * the right layer to run everything at once.
 */
tasks.register("allTests") {
    group = "verification"
    description = "Runs the test task of every module (project-wide test suite)."
    dependsOn(subprojects.map { "${it.path}:test" })
}