package de.polocloud.node.services.factory.template

import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/**
 * Loads all [PlatformTemplate] instances from JSON files found in [cacheDir].
 * Files that fail to deserialize are silently skipped.
 *
 * @param cacheDir Directory containing the platform JSON configuration files.
 * @return List of successfully parsed [PlatformTemplate]s.
 */
fun loadTemplatesFromCache(cacheDir: File = File(".cache/platforms")): List<PlatformTemplate> {
    if (!cacheDir.exists() || !cacheDir.isDirectory) return emptyList()
    return cacheDir.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .mapNotNull { runCatching { json.decodeFromString<PlatformTemplate>(it.readText()) }.getOrNull() }
        .toList()
}
