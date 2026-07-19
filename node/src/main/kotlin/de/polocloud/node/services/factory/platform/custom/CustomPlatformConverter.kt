package de.polocloud.node.services.factory.platform.custom

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import de.polocloud.node.services.factory.platform.PlatformVersionSource

/**
 * Build number attached to every custom platform version. Built-in platforms carry a real
 * build number resolved from their remote API (see
 * [de.polocloud.node.services.factory.template.PlatformTemplateConverter]); a hand-defined
 * custom platform has no such concept, so this is a fixed placeholder shared by every version
 * — [PlatformSourceValidator]/[CustomPlatformService] and [de.polocloud.node.services.factory.process.PlatformJarCache]
 * agree on it so the same cache file name is computed on both the write and the read side.
 */
const val CUSTOM_PLATFORM_BUILD = 0

/** Resolves this entity into the runtime [Platform] model, e.g. for [de.polocloud.node.services.factory.PlatformService]. */
fun CustomPlatform.toPlatform(): Platform = Platform(
    name = name,
    type = type,
    language = language,
    versions = versions.map { it.toPlatformVersion() },
    custom = true
)

private fun CustomPlatformVersion.toPlatformVersion(): PlatformVersion = PlatformVersion(
    version = version,
    build = CUSTOM_PLATFORM_BUILD,
    downloadUrl = if (source == PlatformVersionSource.URL) location else "",
    source = source,
    localFilePath = if (source == PlatformVersionSource.LOCAL_FILE) location else ""
)
