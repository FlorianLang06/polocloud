plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("polocloud-plugin") {
            id = "de.polocloud"
            implementationClass = "de.polocloud.dependency.plugin.PolocloudDependencyPlugin"
        }
    }
}

repositories {
    mavenCentral()
}
