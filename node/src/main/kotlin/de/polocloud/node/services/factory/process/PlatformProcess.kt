package de.polocloud.node.services.factory.process

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import de.polocloud.node.services.factory.template.resolveJavaVersion
import de.polocloud.service.factory.process.JavaRuntimeManager
import de.polocloud.service.factory.process.PlatformRuntime
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

/**
 * Manages downloading and launching a specific [PlatformVersion] of a [Platform].
 *
 * @param platform The platform configuration providing language, global arguments,
 *                 and Java version range breakpoints.
 * @param version  The specific version to download and start.
 */
class PlatformProcess(
    private val platform: Platform,
    private val version: PlatformVersion
) {

    private val logger = LoggerFactory.getLogger(PlatformProcess::class.java)
    private val jarName = "${platform.name}-${version.version}-${version.build}.jar"

    /**
     * Downloads the platform JAR into [targetDir] if not already cached.
     * The download is skipped when a file with the expected name already exists.
     *
     * @param targetDir Directory where the JAR file will be stored.
     * @return The downloaded or already-cached JAR [File].
     */
    fun download(targetDir: File): File {
        targetDir.mkdirs()
        val file = File(targetDir, jarName)
        if (file.exists()) {
            logger.info("$jarName already cached")
            return file
        }
        logger.info("Downloading $jarName ...")
        System.out.flush()
        URI(version.downloadUrl).toURL().openStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        logger.info("done (${file.length() / 1024} KB)")
        return file
    }

    /**
     * Starts the platform process from the given [jarFile].
     *
     * The required Java version is resolved from [Platform.javaVersionRanges].
     * If a range matches, [JavaRuntimeManager] downloads the appropriate JRE
     * automatically. Falls back to the system `java` when no range is configured.
     *
     * Any entries in [environment] are added to the child process environment —
     * this is how the provisioned mTLS identity directory is handed to the
     * service via `POLOCLOUD_IDENTITY_DIR`.
     *
     * @param jarFile The JAR file to execute.
     * @param environment Extra environment variables to expose to the process.
     * @return The running [Process].
     * @throws IllegalStateException if no runtime is registered for the platform language.
     */
    fun start(jarFile: File, environment: Map<String, String> = emptyMap()): Process {
        val executable = resolveExecutable()
        val runtime = PlatformRuntime.forLanguage(platform.language)
        val command = runtime.buildCommand(executable, jarFile, platform.jvmArgs, platform.globalArgs)
        logger.info("Starting ${platform.name} ${version.version} (build ${version.build})")
        return ProcessBuilder(command)
            .directory(jarFile.parentFile)
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
    }

    /**
     * Resolves the java executable path for this version.
     * Downloads the matching JRE via [JavaRuntimeManager] if [Platform.javaVersionRanges]
     * contains a matching breakpoint; otherwise returns `"java"` (system default).
     */
    private fun resolveExecutable(): String {
        val requiredJava = resolveJavaVersion(version.version, platform.javaVersionRanges)
            ?: return "java"
        return JavaRuntimeManager.ensure(requiredJava).absolutePath
    }
}
