plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
}

dependencies {
    implementation(projects.api)
    compileOnly("org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT")
}