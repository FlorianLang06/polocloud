package de.polocloud.dependency.plugin

import de.polocloud.dependency.plugin.dependency.Dependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.attributes
import java.nio.charset.StandardCharsets
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

/**
 * Gradle plugin that embeds a `dependencies.index` file into the produced JAR.
 *
 * The blob contains runtime dependency metadata (group, artifact, version, download URL, checksum)
 * which can later be consumed by Polocloud at runtime to resolve and verify dependencies.
 */
class PolocloudDependencyPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            "polocloud",
            PolocloudDependencyExtension::class.java
        )


        project.afterEvaluate {
            project.tasks.withType(Jar::class.java).configureEach {

                manifest {
                    extension.mainClass?.let { main ->
                        attributes("Main-Class" to main)
                    }

                    attributes(
                        "groupId" to project.group.toString(),
                        "artifactId" to project.name,
                        "version" to project.version.toString()
                    )
                }

                val blobFile = project.layout.buildDirectory
                    .file("dependencies.index")
                    .get()
                    .asFile

                blobFile.parentFile.mkdirs()

                val repositories = project.repositories
                    .filterIsInstance<MavenArtifactRepository>()
                    .map { it.url.toString() }
                    .ifEmpty { listOf("https://repo.maven.apache.org/maven2") }

                doFirst {

                    val dependencies = extension.projects.flatMap { notation ->
                        resolveDependencies(project, notation, repositories)
                    }

                    if (dependencies.isNotEmpty()) {
                        blobFile.writeText(
                            dependencies.joinToString("\n") { it.toNotation() },
                            StandardCharsets.UTF_8
                        )
                    }
                }

                from(blobFile) {
                    into("/")
                }
            }
        }
    }

    private fun resolveDependencies(project: Project, notation: String, repositories: List<String>): List<Dependency> {
        val dependency = project.dependencies.create(notation)
        val config = project.configurations.detachedConfiguration(dependency)

        config.isTransitive = true

        val resolved = config.resolvedConfiguration.resolvedArtifacts

        return resolved.mapNotNull { artifact ->
            val file = artifact.file
            val id = artifact.moduleVersion.id

            val group = id.group
            val name = id.name
            val version = id.version

            val groupPath = group.replace('.', '/')
            val mavenUrl = runCatching {
                resolveMavenUrl(repositories, groupPath, name, version, file)
            }.getOrElse {
                println("[polocloud] Skipping unresolved dependency: $group:$name:$version")
                return@mapNotNull null
            }

            Dependency(
                groupId = group,
                artifactId = name,
                version = version,
                url = mavenUrl,
                checksum = runCatching {
                    fetchChecksum(mavenUrl)
                }.getOrElse {
                    file.inputStream().use {
                        MessageDigest.getInstance("SHA-256")
                            .digest(it.readBytes())
                            .joinToString("") { b -> "%02x".format(b) }
                    }
                }
            )
        }
    }

    /**
     * Resolves a real download URL for the artifact by checking each configured
     * repository in order. Falls back to Maven Central if none respond.
     */
    private fun resolveMavenUrl(
        repositories: List<String>,
        groupPath: String,
        artifactId: String,
        version: String,
        file: File
    ): String {
        for (repoUrl in repositories) {
            val fileName = file.name
            val url = "$repoUrl/$groupPath/$artifactId/$version/$fileName"

            if (version.endsWith("SNAPSHOT")) {
                return resolveSnapshotUrl(repoUrl, groupPath, artifactId, version, file)
            }

            runCatching {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "HEAD"
                connection.connect()
                val code = connection.responseCode
                connection.disconnect()
                if (code == 200) return url
            }
        }

        error("Could not resolve download URL for $artifactId:$version in any configured repository: $repositories")
    }

    /**
     * Resolves the actual JAR URL for a SNAPSHOT version by reading maven-metadata.xml.
     * SNAPSHOT JARs have timestamped filenames like: artifactId-1.0.0-20241201.123456-1.jar
     */
    private fun resolveSnapshotUrl(
        repoUrl: String,
        groupPath: String,
        artifactId: String,
        version: String,
        file: File
    ): String {
        val fileName = file.name
        return "$repoUrl/$groupPath/$artifactId/$version/$fileName"
    }
}

/**
 * Attempts to fetch a checksum for the given artifact URL.
 *
 * Prefers SHA-256 and falls back to SHA-1 if unavailable.
 *
 * @param jarUrl the base URL of the JAR artifact
 * @return the checksum value as a hexadecimal string
 * @throws IllegalStateException if no checksum could be resolved
 */
fun fetchChecksum(jarUrl: String): String {

    fun load(url: String): String = URI(url).toURL().readText().trim().split(" ")[0]

    return runCatching {
        load("$jarUrl.sha256")
    }.getOrElse {
        load("$jarUrl.sha1")
    }
}

/**
 * Adds a runtime dependency that will be embedded into the `dependencies.index`
 * and also registers it as an `implementation` dependency.
 *
 * @param notation dependency notation (`group:artifact:version`)
 */

fun Project.polocloudRuntime(notation: Any) {

    val extension = extensions.getByType(PolocloudDependencyExtension::class.java)

    when (notation) {

        is Provider<*> -> {
            notation.map { dep ->
                if (dep is MinimalExternalModuleDependency) {
                    val version = dep.versionConstraint.requiredVersion
                        .takeIf { it.isNotBlank() }
                        ?: dep.versionConstraint.displayName

                    val gav = "${dep.module.group}:${dep.module.name}:$version"

                    extension.projects.add(gav)
                }
            }.get()
            dependencies.add("implementation", notation)
        }

        else -> {
            extension.projects.add(notation.toString())
            dependencies.add("implementation", notation)
        }
    }
}
