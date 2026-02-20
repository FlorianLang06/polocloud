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
    compileOnly("redis.clients:jedis:7.2.1")
    compileOnly("org.mongodb:mongodb-driver-sync:5.6.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    runtimeOnly(projects.common)
    runtimeOnly(libs.bundles.logging)
    runtimeOnly(libs.hikariCp)
    runtimeOnly(libs.postgreSql)
    runtimeOnly(libs.polocloud.i18n)
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    runtimeOnly("org.mongodb:mongodb-driver-sync:4.11.0")
    runtimeOnly("com.mysql:mysql-connector-j:9.6.0")
    runtimeOnly("redis.clients:jedis:7.2.1")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    runtimeOnly("org.junit.platform:junit-platform-suite:1.10.0")
    runtimeOnly("com.h2database:h2:2.4.240")
    runtimeOnly("org.mongodb:mongodb-driver-sync:5.6.3")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation(libs.gson)
    testImplementation(projects.common)
    testImplementation(libs.bundles.logging)
    testImplementation(libs.hikariCp)
    testImplementation(libs.postgreSql)
    testImplementation(libs.polocloud.i18n)
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")     // Test API
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")      // Engine zum Ausführen
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")  // Optional: für @ParameterizedTest
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:mongodb:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:mariadb:1.21.4")
    testImplementation("org.testcontainers:cassandra:1.21.4")

    testImplementation("org.mongodb:mongodb-driver-sync:4.11.0")
    testImplementation("com.mysql:mysql-connector-j:9.6.0")
    testImplementation("redis.clients:jedis:7.2.1")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation("org.mongodb:mongodb-driver-sync:5.6.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}