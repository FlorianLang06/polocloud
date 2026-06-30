package de.polocloud.node.services.factory.template

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

/**
 * Converts a list of [PlatformTemplate]s into fully resolved [Platform]s by
 * fetching available versions from the remote API defined in each template.
 *
 * @param items Templates loaded from the cache directory.
 * @return Platforms with their [PlatformVersion] lists populated.
 */
fun convertTemplatesToPlatform(items: List<PlatformTemplate>): List<Platform> {
    return items.map { template ->
        Platform(
            name = template.name,
            type = template.type,
            language = template.language,
            jvmArgs = template.jvmArgs,
            globalArgs = template.globalArgs,
            javaVersionRanges = template.javaVersionRanges,
            versions = scanVersions(template)
        )
    }
}

/**
 * Fetches and resolves all available [PlatformVersion]s for the given [template].
 * Returns an empty list if the detection mode is not AUTOMATIC or if any request fails.
 */
private fun scanVersions(template: PlatformTemplate): List<PlatformVersion> {
    val detection = template.versionDetection
    if (detection.mode != "AUTOMATIC") return emptyList()
    return runCatching {
        val rootJson = fetchJson(detection.baseUrl)
        val versions = resolveAllVersions(rootJson, detection.parse.versionPath)
        versions.mapNotNull { version -> fetchVersionDetails(detection, version) }
    }.getOrDefault(emptyList())
}

/**
 * Fetches the latest build details for a single [version] using the given [detection] config.
 *
 * The download URL is resolved either via [VersionParse.downloadPath] (direct JSON path)
 * or by substituting placeholders in [VersionParse.downloadUrl].
 *
 * @return A [PlatformVersion] for the given version, or null if resolution fails.
 */
private fun fetchVersionDetails(detection: VersionDetection, version: String): PlatformVersion? {
    return runCatching {
        val buildUrl = detection.parse.buildUrl
            .replace("{baseUrl}", detection.baseUrl)
            .replace("{version}", version)
        val buildJson = fetchJson(buildUrl)
        val build = resolveElement(buildJson, detection.parse.buildPath)
            ?.jsonPrimitive?.int ?: return null
        val downloadUrl = if (detection.parse.downloadPath != null) {
            resolveElement(buildJson, detection.parse.downloadPath)
                ?.jsonPrimitive?.content ?: return null
        } else {
            detection.parse.downloadUrl
                .replace("{baseUrl}", detection.baseUrl)
                .replace("{version}", version)
                .replace("{build}", build.toString())
        }
        PlatformVersion(version, build, downloadUrl)
    }.getOrNull()
}

/**
 * Fetches the content of [url] and parses it as a [JsonElement].
 *
 * @throws Exception if the HTTP request fails or the response is not valid JSON.
 */
private fun fetchJson(url: String): JsonElement {
    val text = URI(url).toURL().readText()
    return Json.parseToJsonElement(text)
}
