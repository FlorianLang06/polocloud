package de.polocloud.factory.platform

/**
 * Represents a single downloadable build of a [Platform].
 *
 * @param version     Version string as returned by the API (e.g. "1.21.4", "26.1.2").
 * @param build       Build number identifying the specific artifact within a version.
 * @param downloadUrl Direct URL to the JAR file for this build.
 */
data class PlatformVersion(
    val version: String,
    val build: Int,
    val downloadUrl: String
)
