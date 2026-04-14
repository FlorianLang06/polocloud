package de.polocloud.node.services

import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.services.control.ServiceControlPlan
import de.polocloud.node.services.process.ServiceProcess
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.jar.JarFile
import kotlin.io.path.*

/**
 * Factory responsible for discovering and bootstrapping services.
 *
 * Responsibilities:
 * - Scanning the local cache directory for service JARs
 * - Extracting metadata from JAR manifests
 * - Creating [ServiceHolder] instances
 * - Booting services based on a given [ServiceControlPlan]
 */
object ServiceFactory {

    private val logger = LoggerFactory.getLogger(ServiceFactory::class.java)

    /**
     * Directory where service JARs are stored.
     */
    private val path = Path("local/services/cache")

    /**
     * Scans the local cache directory for service JAR files.
     *
     * Each JAR must contain a manifest with:
     * - `artifactId`
     * - `version`
     *
     * @return a list of discovered [ServiceHolder] instances
     */
    fun scanServices(): List<ServiceHolder> {
        if (!path.exists()) {
            Files.createDirectories(path)
            return emptyList()
        }

        return path.listDirectoryEntries("*.jar").mapNotNull { jarPath ->
            try {
                JarFile(jarPath.toFile()).use { jarFile ->
                    val attributes = jarFile.manifest?.mainAttributes

                    val artifactId = attributes?.getValue("artifactId")
                    val version = attributes?.getValue("version")

                    if (artifactId == null || version == null) {
                        logger.warn(
                            "Skipping '{}': manifest missing 'artifactId' or 'version'",
                            jarPath.fileName
                        )
                        return@mapNotNull null
                    }

                    ServiceHolder(artifactId, version, jarPath.toFile())
                }
            } catch (ex: Exception) {
                logger.error("Failed to read JAR '{}'", jarPath.fileName, ex)
                null
            }
        }
    }

    /**
     * Boots a new service instance based on the given plan and service definition.
     *
     * Steps:
     * 1. Create a [ServiceProcess] representation
     * 2. Prepare a container directory
     * 3. Copy the service JAR into the container
     * 4. Start a new JVM process running the service
     *
     * @param plan the control plan defining how the service should run
     * @param holder the service definition containing artifact metadata and file reference
     */
    fun bootService(plan: ServiceControlPlan, holder: ServiceHolder) {
        val serviceProcess = ServiceProcess(
            UUID.randomUUID(),
            plan.name,
            NodeEnvironment.runtime.nodeId.get(),
            -1,
            -1,
            ServiceState.LOADING
        )

        val container = ServiceContainer(1, serviceProcess)
        val workingDir = container.path()

        try {
            Files.createDirectories(workingDir)

            val targetJar = workingDir.resolve(holder.file.name)

            logger.info(
                "Starting service '{}' with plan '{}' in '{}'",
                container.name(),
                plan.name,
                workingDir
            )

            // Copy service JAR into container (overwrite if exists)
            Files.copy(
                holder.file.toPath(),
                targetJar,
                StandardCopyOption.REPLACE_EXISTING
            )

            val processBuilder = ProcessBuilder(
                "java",
                "-jar",
                targetJar.fileName.toString()
            ).directory(workingDir.toFile()).inheritIO()

            serviceProcess.changeState(ServiceState.BOOTING)

            val process = processBuilder.start()

            serviceProcess.withRuntime(process)

        } catch (ex: Exception) {
            logger.error(
                "Failed to start service '{}' with plan '{}'",
                holder.name,
                plan.name,
                ex
            )
            serviceProcess.changeState(ServiceState.FAILED)
        }
    }
}