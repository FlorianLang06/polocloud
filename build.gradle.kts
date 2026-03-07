
allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "dev.httpmarco.polocloud"
    // version is now set by gradle/version.gradle.kts — do NOT set it here

    repositories {
        mavenCentral()

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}