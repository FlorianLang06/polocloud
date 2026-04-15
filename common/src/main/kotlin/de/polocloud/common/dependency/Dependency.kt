package de.polocloud.common.dependency

import de.polocloud.common.dependency.checksum.FileChecksum.sha1
import de.polocloud.common.dependency.checksum.FileChecksum.sha256
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 120_000

/**
 * Represents a downloadable dependency (e.g., a JAR) with its Maven coordinates and checksum.
 *
 * @property groupId the group ID of the dependency (e.g., "org.example")
 * @property artifactId the artifact ID of the dependency (e.g., "example-lib")
 * @property version the version of the dependency (e.g., "1.0.0")
 * @property url the URL from which the dependency can be downloaded
 * @property checksum the expected SHA-1 or SHA-256 checksum of the dependency file
 */
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val url: String,
    val checksum: String
) {

    companion object {
        // Global lock map shared across all Dependency instances so that two registries
        // scanning the same coordinates never download the same JAR concurrently.
        private val downloadLocks = ConcurrentHashMap<String, Any>()

        private val logger = LoggerFactory.getLogger(Dependency::class.java)
    }

    /**
     * Downloads the dependency JAR to the global cache if it is absent or its checksum is invalid.
     *
     * The file is stored in `.cache/dependencies/<groupId>/<artifactId>/<version>/<artifactId>-<version>.jar`.
     * A [CONNECT_TIMEOUT_MS] connect timeout and [READ_TIMEOUT_MS] read timeout are applied so a
     * slow or dead mirror never hangs the process indefinitely.
     *
     * @throws IllegalStateException if the download fails or the checksum does not match
     */
    fun download() {
        if (url == "unknown") {
            logger.debug("Skipping {}:{} — no download URL", artifactId, version)
            return
        }

        val lock = downloadLocks.getOrPut("$groupId:$artifactId:$version") { Any() }

        synchronized(lock) {
            val target = localPath()

            if (target.exists()) {
                if (verifyChecksum(target)) {
                    logger.debug("Cache hit: {}:{}", artifactId, version)
                    return
                }
                logger.warn("Checksum mismatch for cached {}:{} — re-downloading", artifactId, version)
                target.deleteIfExists()
            }

            Files.createDirectories(target.parent)
            val tempFile = Files.createTempFile(target.parent, "$artifactId-$version", ".tmp")

            logger.info("Downloading {}:{}", artifactId, version)
            val start = System.currentTimeMillis()

            try {
                val connection = URI(url).toURL().openConnection().apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }
                connection.getInputStream().use { input ->
                    Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (ex: Exception) {
                tempFile.deleteIfExists()
                error("Failed to download $artifactId:$version from $url — ${ex.message}")
            }

            if (!verifyChecksum(tempFile)) {
                tempFile.deleteIfExists()
                error("Checksum verification failed for $artifactId:$version")
            }

            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            logger.info("Downloaded {}:{} in {} ms", artifactId, version, System.currentTimeMillis() - start)
        }
    }

    fun localPath(): Path {
        return Path.of(".cache/dependencies")
            .resolve(convertedPathGroupId())
            .resolve(artifactId)
            .resolve(version)
            .resolve(fileName())
    }

    /**
     * Verifies the checksum of [filePath].
     *
     * The algorithm is inferred from the checksum length (40 hex chars → SHA-1, 64 → SHA-256)
     * so the file is only read once instead of twice.
     */
    private fun verifyChecksum(filePath: Path): Boolean {
        val file = filePath.toFile()
        return when (checksum.length) {
            40   -> file.sha1().equals(checksum, ignoreCase = true)
            64   -> file.sha256().equals(checksum, ignoreCase = true)
            else -> file.sha1().equals(checksum, ignoreCase = true)
                 || file.sha256().equals(checksum, ignoreCase = true)
        }
    }

    fun fileName(): String = "$artifactId-$version.jar"

    private fun convertedPathGroupId(): String = groupId.replace('.', '/')
}
