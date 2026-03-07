package dev.httpmarco.polocloud.dependency.plugin

import dev.httpmarco.polocloud.dependency.plugin.dependency.Dependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.attributes
import java.nio.charset.StandardCharsets
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import java.net.HttpURLConnection
import java.net.URI

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
        val detached = project.configurations.detachedConfiguration(dependency)
        val resolved = detached.resolvedConfiguration

        return resolved.resolvedArtifacts.map { artifact ->
            val file = artifact.file
            val group = artifact.moduleVersion.id.group
            val name = artifact.name
            val version = artifact.moduleVersion.id.version

            val groupPath = group.replace('.', '/')
            val mavenUrl = resolveMavenUrl(repositories, groupPath, name, version)

            Dependency(
                groupId = group,
                artifactId = name,
                version = version,
                url = mavenUrl,
                checksum = file.inputStream().use {
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(it.readBytes())
                        .joinToString("") { b -> "%02x".format(b) }
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
        version: String
    ): String {
        val fileName = "$artifactId-$version.jar"

        for (repoUrl in repositories) {
            val url = "$repoUrl/$groupPath/$artifactId/$version/$fileName"
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
                    val gav =
                        "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.requiredVersion}"

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
