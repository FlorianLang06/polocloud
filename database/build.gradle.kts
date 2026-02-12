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

    testImplementation(kotlin("test"))
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")     // Test API
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")      // Engine zum Ausführen
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")  // Optional: für @ParameterizedTest
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:mongodb:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:redis:1.21.4")
    testImplementation("org.testcontainers:mariadb:1.21.4")

    testImplementation("org.mongodb:mongodb-driver-sync:4.11.0")
    testImplementation("mysql:mysql-connector-java:8.1.0")
    testImplementation("redis.clients:jedis:4.4.3")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.2.0")
}

tasks.test {
    useJUnitPlatform()
}
