package de.polocloud.node.services.factory

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.template.convertTemplatesToPlatform
import de.polocloud.node.services.factory.template.loadTemplatesFromCache
import org.slf4j.LoggerFactory

class PlatformService {

    private val logger = LoggerFactory.getLogger(PlatformService::class.java)
    private val platforms = mutableMapOf<String, Platform>()

    fun load() {
        val templates = loadTemplatesFromCache()
        if (templates.isEmpty()) {
            logger.warn("No platform templates found in .cache/platforms/")
            return
        }
        val resolved = convertTemplatesToPlatform(templates)
        resolved.forEach { platforms[it.name] = it }
        logger.info("Loaded {} platform(s): {}", platforms.size, platforms.keys.joinToString())
    }

    fun find(name: String): Platform? = platforms[name]
}
