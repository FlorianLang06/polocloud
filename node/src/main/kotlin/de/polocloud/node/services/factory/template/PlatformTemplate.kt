package de.polocloud.node.services.factory.template

import de.polocloud.node.services.factory.platform.JavaVersionRange
import kotlinx.serialization.Serializable

/**
 * Represents a platform configuration loaded from a cache JSON file.
 *
 * @param name            Unique platform identifier (e.g. "paper", "velocity").
 * @param type            Platform role: SERVER or PROXY.
 * @param language        Runtime language used to launch the platform (e.g. JAVA).
 * @param jvmArgs         JVM arguments placed before `-jar` (e.g. "-Dcom.mojang.eula.agree=true").
 * @param globalArgs      JVM and program arguments passed when starting a process.
 * @param tasks               Optional tasks applied to specific version ranges.
 * @param javaVersionRanges   Ordered breakpoints mapping platform version ranges to required Java versions.
 * @param versionDetection    Configuration describing how versions are fetched from a remote API.
 */
@Serializable
data class PlatformTemplate(
    val name: String,
    val type: String,
    val language: String,
    val jvmArgs: List<String> = emptyList(),
    val globalArgs: List<String> = emptyList(),
    val tasks: List<ServiceTask>,
    val javaVersionRanges: List<JavaVersionRange> = emptyList(),
    val versionDetection: VersionDetection
)

/**
 * A named task that targets a specific range of platform versions.
 *
 * @param name     Human-readable task identifier.
 * @param versions Version constraint expression (e.g. ">1.13 && <1.21").
 */
@Serializable
data class ServiceTask(
    val name: String,
    val versions: String
)

/**
 * Describes how versions are detected for a platform.
 *
 * @param mode    Detection strategy. Only AUTOMATIC is currently supported.
 * @param baseUrl Root API URL used as the starting point for all requests.
 * @param parse   Rules defining how to extract versions and build artifacts from the API.
 */
@Serializable
data class VersionDetection(
    val mode: String,
    val baseUrl: String,
    val parse: VersionParse
)

/**
 * Defines the JSON parsing rules used to locate versions and builds from a remote API.
 *
 * @param type          Response format (currently only JSON is supported).
 * @param versionPath   JSON path to the version list in the base URL response.
 * @param buildUrl      URL template for fetching builds of a specific version.
 * @param buildPath     JSON path to the build number within the build response.
 * @param downloadUrl   URL template for constructing the download link (used when [downloadPath] is null).
 * @param downloadPath  JSON path to the download URL directly within the build response.
 *                      When set, [downloadUrl] is ignored.
 */
@Serializable
data class VersionParse(
    val type: String,
    val versionPath: String,
    val buildUrl: String,
    val buildPath: String,
    val downloadUrl: String = "",
    val downloadPath: String? = null
)
