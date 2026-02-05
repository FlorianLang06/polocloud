plugins {
    id("java")
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.runner.PolocloudRuntimeLauncher"
}

tasks.named<Jar>("jar") {
    val subprojects = listOf(
        ":cli",
        ":common",
        ":console",
        ":installer"
    )

    dependsOn(subprojects.map { "$it:jar" })

    subprojects.forEach { path ->
        val jarTask = project(path).tasks.named<Jar>("jar")

        from(jarTask.flatMap { it.archiveFile }) {
            into(".cache")
        }
    }
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}