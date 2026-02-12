plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

polocloud {

}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly(libs.bundles.logging)
    compileOnly(projects.common)
    compileOnly(libs.postgreSql)
    compileOnly(libs.hikariCp)
    compileOnly(libs.polocloud.i18n)

    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
    runtimeOnly(libs.postgreSql)
    runtimeOnly(libs.polocloud.i18n)
}