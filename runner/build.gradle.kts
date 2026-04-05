plugins {
    java

    id("de.polocloud")
}

polocloud {
    mainClass = "de.polocloud.runner.PolocloudRuntimeLauncher"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Enable-Native-Access" to "ALL-UNNAMED",

            "kotlin-version" to libs.versions.kotlin.jvm.get(),
        )
    }

    val subprojects = listOf(
        ":common",
        ":cli",
        ":node"
    )

    dependsOn(subprojects.map { "$it:jar" })

    subprojects.forEach { path ->
        val jarTask = project(path).tasks.named<Jar>("jar")

        from(jarTask.flatMap { it.archiveFile }) {
            into(".cache/dependencies")
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