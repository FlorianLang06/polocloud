package de.polocloud.node.services.factory

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.task.TaskDefinition
import de.polocloud.node.services.factory.task.loadTaskDefinitionsFromCache
import de.polocloud.node.services.factory.template.PlatformDownloader
import de.polocloud.node.services.factory.template.convertTemplatesToPlatform
import de.polocloud.node.services.factory.template.loadTemplatesFromCache
import org.slf4j.LoggerFactory
import java.io.File

class PlatformService {

    private val logger = LoggerFactory.getLogger(PlatformService::class.java)
    private val platforms = mutableMapOf<String, Platform>()
    private val taskDefinitions = mutableMapOf<String, TaskDefinition>()
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

        taskDefinitions.putAll(loadTaskDefinitionsFromCache(cacheDir))

        logger.info("Loaded {} platform(s): {}", platforms.size, platforms.keys.joinToString())
        if (taskDefinitions.isNotEmpty()) {
            logger.info("Loaded {} task definition(s): {}", taskDefinitions.size, taskDefinitions.keys.joinToString())
        }
    }

    fun find(name: String): Platform? = platforms[name]

    fun all(): Collection<Platform> = platforms.values

    /** The platform's own cache directory (e.g. `.cache/platforms/velocity`), used as
     *  the source for [de.polocloud.node.services.factory.task.TaskStepType.COPY_FILE_IF_NOT_EXISTS] steps. */
    fun directoryFor(platform: Platform): File = File(cacheDir, platform.name)

    /** All loaded task definitions, keyed by [TaskDefinition.key]. */
    fun taskDefinitions(): Map<String, TaskDefinition> = taskDefinitions
}
