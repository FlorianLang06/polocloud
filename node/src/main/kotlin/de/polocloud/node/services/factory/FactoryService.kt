package de.polocloud.node.services.factory

import de.polocloud.node.group.Group
import de.polocloud.node.services.Service
import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.process.PlatformProcess
import de.polocloud.node.services.factory.template.convertTemplatesToPlatform
import de.polocloud.node.services.factory.template.loadTemplatesFromCache
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

class FactoryService {

    private val logger = LoggerFactory.getLogger(FactoryService::class.java)

    @Deprecated("remove")
    private val platforms: Map<String, Platform> by lazy {
        val templates = loadTemplatesFromCache()
        convertTemplatesToPlatform(templates).associateBy { it.name }.also { map ->
            if (map.isEmpty()) {
                logger.warn("No platform templates found in cache/ — services cannot be started")
            } else {
                logger.info("Loaded {} platform(s): {}", map.size, map.keys.joinToString())
            }
        }
    }

    // uuid → (service, process) for all currently alive services
    private val running = mutableMapOf<UUID, Pair<Service, Process>>()

    fun start(service: Service, group: Group) {
        val platform = platforms[group.platform]
            ?: throw IllegalArgumentException("Platform '${group.platform}' is not loaded")
        val version = platform.versions.find { it.version == group.version }
            ?: throw IllegalArgumentException(
                "Version '${group.version}' not available for platform '${group.platform}'"
            )

        val workDir = File("servers/${group.name}/${service.index}")
        val process = PlatformProcess(platform, version)
        val jar = process.download(workDir)
        val proc = process.start(jar)

        running[service.id] = Pair(service, proc)
        logger.info("Service {}-{} started (pid: {})", group.name, service.index, proc.pid())
    }

    fun runningCount(groupName: String): Long {
        pruneDeadProcesses()
        return running.values.count { it.first.group == groupName }.toLong()
    }

    fun runningIndexes(groupName: String): Set<Int> {
        pruneDeadProcesses()
        return running.values
            .filter { it.first.group == groupName }
            .map { it.first.index }
            .toSet()
    }

    private fun pruneDeadProcesses() {
        running.entries.removeIf { !it.value.second.isAlive }
    }
}
