plugins {
    `maven-publish`
    signing

    alias(libs.plugins.nexus.publish)
}

allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "de.polocloud"
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

subprojects {
    apply(plugin = "maven-publish")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        publishing {
            publications {
                create<MavenPublication>("maven") {
                    if (project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                        from(components["java"])

                        pom {
                            name.set("polocloud-i18n")
                            description.set("Dynamic translation system for the PoloCloud ecosystem.")
                            url.set("https://github.com/thePolocloud/polocloud-i18n")

                            licenses {
                                license {
                                    name.set("Apache-2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                                }
                            }

                            developers {
                                developer {
                                    id.set("httpmarco")
                                    name.set("Mirco Lindenau")
                                    email.set("mirco.lindenau@gmx.de")
                                }
                            }

                            scm {
                                url.set("https://github.com/thePolocloud/polocloud-i18n")
                                connection.set("scm:git:https://github.com/thePolocloud/polocloud-i18n.git")
                                developerConnection.set("scm:git:https://github.com/thePolocloud/polocloud-i18n.git")
                            }
                        }
                    }
                }
            }
        }

    }
}


nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassphrase = System.getenv("GPG_PASSPHRASE")

    if (signingKey != null && signingPassphrase != null) {
        useInMemoryPgpKeys(signingKey, signingPassphrase)
        sign(publishing.publications)
    }
}