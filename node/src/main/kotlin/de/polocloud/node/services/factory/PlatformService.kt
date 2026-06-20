package de.polocloud.node.services.factory

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.template.PlatformDownloader
import de.polocloud.node.services.factory.template.convertTemplatesToPlatform
import de.polocloud.node.services.factory.template.loadTemplatesFromCache
import org.slf4j.LoggerFactory
import java.io.File

class PlatformService {

    private val logger = LoggerFactory.getLogger(PlatformService::class.java)
    private val platforms = mutableMapOf<String, Platform>()
    private val cacheDir = File(".cache/platforms")

    fun load() {
        var templates = loadTemplatesFromCache(cacheDir)
        if (templates.isEmpty()) {
            logger.info("No platform templates found in {} — fetching latest release ...", cacheDir.path)
            runCatching { PlatformDownloader.downloadInto(cacheDir) }
                .onFailure { logger.error("Failed to download platform templates", it); return }
            templates = loadTemplatesFromCache(cacheDir)
        }
        if (templates.isEmpty()) {
            logger.warn("No platform templates available in {}", cacheDir.path)
            return
        }
        val resolved = convertTemplatesToPlatform(templates)
        resolved.forEach { platforms[it.name] = it }
        logger.info("Loaded {} platform(s): {}", platforms.size, platforms.keys.joinToString())
    }

    fun find(name: String): Platform? = platforms[name]
}
