
allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "dev.httpmarco.polocloud"
    // version is now set by gradle/version.gradle.kts — do NOT set it here

    repositories {
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2") }

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}