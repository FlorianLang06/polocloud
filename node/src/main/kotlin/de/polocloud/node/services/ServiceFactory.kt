package de.polocloud.node.services

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.StringArgumentInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import de.polocloud.common.system.PolocloudSystemProperties
import de.polocloud.node.utils.IndexGenerator
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.services.control.ServiceControlPlan
import de.polocloud.node.services.process.ServiceProcess
import de.polocloud.node.services.process.ServiceProcessRepository
import de.polocloud.proto.ServiceState
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * Factory responsible for discovering and bootstrapping services.
 *
 * Responsibilities:
 * - Scanning the local cache directory for service JARs
 * - Extracting metadata from JAR manifests
 * - Creating [ServiceHolder] instances
 * - Booting services based on a given [ServiceControlPlan]
 *
 * Each service process receives the following system properties so it can
 * establish an mTLS channel back to this node:
 *
 * | Property       | Value                                                  |
 * |----------------|--------------------------------------------------------|
 * | `service.id`   | UUID of the [ServiceProcess]                           |
 * | `service.name` | Human-readable container name (e.g. `lobby-1`)         |
 * | `node.address` | `host:port` of the node's gRPC endpoint                |
 * | `service.cert` | Absolute path to the service's signed certificate PEM  |
 * | `service.key`  | Absolute path to the service's private key PEM         |
 * | `service.ca`   | Absolute path to the CA certificate PEM                |
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
                    val mainClass = attributes?.getValue("Main-Class")

                    if (artifactId == null || version == null) {
                        logger.warn(
                            "Skipping '{}': manifest missing 'artifactId' or 'version'",
                            jarPath.fileName
                        )
                        return@mapNotNull null
                    }

                    ServiceHolder(artifactId, version, jarPath.toFile(), mainClass)
                }
            } catch (ex: Exception) {
                logger.error("Failed to read JAR '{}'", jarPath.fileName, ex)
                null
            }
        }
    }

    /**
     * Boots a new service instance.
     *
     * In addition to the standard JVM classpath setup the process is given
     * four TLS-related system properties so [de.polocloud.services.sdk.communication.NodeConnection]
     * can open an mTLS channel back to this node without any additional configuration.
     */
    fun bootService(plan: ServiceControlPlan, holder: ServiceHolder): ServiceContainer {
        val serviceId = UUID.randomUUID()

        val serviceProcess = ServiceProcess(
            serviceId,
            IndexGenerator.generateService(),
            plan.name,
            NodeEnvironment.runtime.nodeId.get(),
            -1,
            -1,
            ServiceState.LOADING,
        )

        // Persist BEFORE booting the JVM — ServiceRegistrationService needs
        // this entry to exist when the service sends its CSR.
        ServiceProcessRepository.update(serviceProcess)

        val container = ServiceContainer(serviceProcess.index, serviceProcess)
        val workingDir = container.path()
        val identityDir = workingDir.resolve(".identity")

        try {
            Files.createDirectories(workingDir)

            val targetJar = workingDir.resolve(holder.file.name)

            logger.info(
                "Starting service '{}' with plan '{}' in '{}'",
                container.name(),
                plan.name,
                workingDir,
            )

            Files.copy(holder.file.toPath(), targetJar, StandardCopyOption.REPLACE_EXISTING)

            val dependencyPaths = loadDependencies(holder, workingDir.resolve(".dependencies"))

            val classpath = buildList {
                add(targetJar.toAbsolutePath().toString())
                add(File(System.getProperty(PolocloudSystemProperties.COMMON_PATH)).absolutePath)
                add(File(System.getProperty(PolocloudSystemProperties.PROTO_PATH)).absolutePath)
                addAll(dependencyPaths)
            }.joinToString(File.pathSeparator)

            val token = NodeEnvironment.runtime.tokenManager.issue(serviceId)

            val nodeConfig = NodeEnvironment.configurations
            val nodeGrpcAddress = "${nodeConfig.general.bindAddress.hostname}:${nodeConfig.general.bindAddress.port}"
            val nodeRegistrationAddress = nodeConfig.cluster.registration.let { "${it.hostname}:${it.port}" }

            val processBuilder = ProcessBuilder(
                "java",
                "-Dservice.id=$serviceId",
                "-Dservice.name=${container.name()}",
                "-Dservice.token=$token",
                "-Dservice.identity.dir=${identityDir.toAbsolutePath()}",
                "-Dnode.address=$nodeGrpcAddress",
                "-Dnode.registration.address=$nodeRegistrationAddress",
                "-cp", classpath,
                "de.polocloud.services.sdk.ServiceBootKt",
            )

            processBuilder.directory(workingDir.toFile())
            serviceProcess.changeState(ServiceState.BOOTING)

            val process = processBuilder.start()
            serviceProcess.withRuntime(process)

        } catch (ex: Exception) {
            logger.error(
                "Failed to start service '{}' with plan '{}'",
                holder.name,
                plan.name,
                ex,
            )
            serviceProcess.changeState(ServiceState.FAILED)
        }

        return container
    }

    fun shutdown(container: ServiceContainer) {
        val process = container.process
        val handle = ProcessHandle.of(container.process.pid.toLong()).orElse(null)

        if (handle == null || !handle.isAlive) {
            logger.warn(
                "No alive process found for service '{}' (pid {})",
                process.plan,
                process.pid,
            )
        } else {
            logger.info("Stopping service '{}' (pid {})", process.plan, process.pid)

            handle.destroy()
            try {
                handle.onExit().get(10, TimeUnit.SECONDS)
            } catch (_: Exception) {
                logger.warn("Service '{}' did not stop gracefully — force-killing", process.plan)
                handle.destroyForcibly()
            }
        }

        ServiceProcessRepository.delete(process)

        val workingDir = ServiceContainer(1, process).path()
        try {
            Files.walk(workingDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        } catch (ex: Exception) {
            logger.error(
                "Failed to delete working directory '{}' for service '{}'",
                workingDir,
                process.uuid,
                ex,
            )
        }

        logger.info("Service '{}' stopped and cleaned up", process.plan)
    }

    /**
     * Resolves and downloads all dependencies declared in the [holder]'s JAR into the
     * global cache (`.cache/dependencies/`), then copies each JAR into [instanceDepDir]
     * so every service instance has its own isolated copy.
     *
     * The JAR is expected to contain a `dependencies.index` file listing each
     * dependency in the format used by [OwnBlobScanner]. If no index is present,
     * an empty list is returned and the service is started without additional classpath entries.
     *
     * @param holder the service whose embedded dependency index should be resolved
     * @param instanceDepDir the `.dependencies` folder inside the service instance directory
     * @return absolute filesystem paths of the dependency JARs inside [instanceDepDir]
     */
    private fun loadDependencies(holder: ServiceHolder, instanceDepDir: Path): List<String> {
        val registry = DependencyRegistry(StringArgumentInsert())
        registry.scan(OwnBlobScanner(holder.file))
        registry.downloadAll()

        // Copy from global cache into the per-instance .dependencies folder,
        // preserving the full cache structure: <group>/<artifact>/<version>/<artifact>-<version>.jar
        val cacheRoot = Path(".cache/dependencies").toAbsolutePath()
        Files.createDirectories(instanceDepDir)

        return registry.collect().mapNotNull { cachePath ->
            val source = Path(cachePath).toAbsolutePath()

            if (!Files.exists(source)) {
                return@mapNotNull null
            }

            val relative = cacheRoot.relativize(source)
            val target = instanceDepDir.resolve(relative)

            Files.createDirectories(target.parent)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)

            target.toAbsolutePath().toString()
        }
    }
}