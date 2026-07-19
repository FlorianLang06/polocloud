package de.polocloud.node.services.factory.platform

/**
 * Represents a single downloadable build of a [Platform].
 *
 * @param version       Version string as returned by the API (e.g. "1.21.4", "26.1.2").
 * @param build         Build number identifying the specific artifact within a version.
 * @param downloadUrl   Direct URL to the JAR file for this build. Only meaningful when
 *                       [source] is [PlatformVersionSource.URL].
 * @param source        Where the jar comes from. Always [PlatformVersionSource.URL] for
 *                       built-in platforms.
 * @param localFilePath The node-local path the jar was copied from when [source] is
 *                       [PlatformVersionSource.LOCAL_FILE], kept only for diagnostics —
 *                       the jar itself already lives in the shared cache by the time this
 *                       is set (see `de.polocloud.node.services.factory.platform.custom.CustomPlatformService`).
 */
data class PlatformVersion(
    val version: String,
    val build: Int,
    val downloadUrl: String,
    val source: PlatformVersionSource = PlatformVersionSource.URL,
    val localFilePath: String = ""
)
