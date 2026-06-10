package de.polocloud.node.services.factory

import de.polocloud.node.group.Group
import de.polocloud.node.services.Service
import de.polocloud.node.services.factory.process.PlatformProcess
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

class FactoryService(private val platformService: PlatformService) {

    private val logger = LoggerFactory.getLogger(FactoryService::class.java)

    // uuid → (service, process) for all currently alive services
    private val running = mutableMapOf<UUID, Pair<Service, Process>>()

    fun start(service: Service, group: Group) {
        val platform = platformService.find(group.platform)
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
