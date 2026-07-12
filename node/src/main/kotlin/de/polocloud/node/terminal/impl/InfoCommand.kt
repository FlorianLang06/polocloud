package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.os.SystemResources
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dumps every piece of information the node holds about itself: identity, version,
 * network configuration, live + reported resource usage, process/runtime details and
 * the workload currently placed on this node.
 *
 * Reuses the same section shape as [ClusterCommand.info], but bound to the local node
 * and extended with data that isn't part of the replicated [de.polocloud.node.cluster.node.NodeData]
 * (live JVM/OS stats, full version metadata, resolved network configuration).
 */
class InfoCommand(
    private val localNodeContainer: LocalNodeContainer,
    private val holder: ConfigurationHolder<NodeConfigurations>,
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
) : Command("info", "Show detailed information about this local node") {

    private val logger = LoggerFactory.getLogger(InfoCommand::class.java)

    init {
        defaultExecution { info() }
    }

    private fun info() {
        val node = localNodeContainer.data
        val config = holder.value
        val heartbeat = NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }
        val runtimeMx = ManagementFactory.getRuntimeMXBean()
        val localServices = serviceProvider.localServices

        logger.info("Node ${node.name()}:")
        logger.info("  id: ${node.id}")
        logger.info("  state: ${node.state}")
        logger.info("  head: ${if (node.head) "yes (since ${node.electedAt})" else "no"}")
        logger.info("  cluster registration: ${node.hostname}:${node.port}")
        logger.info("  first connection: ${node.firstConnection}")
        logger.info("  last connection: ${node.lastConnection}")

        logger.info("Version:")
        logger.info("  running: ${PolocloudVersion.CURRENT.toDisplayString()}")
        logger.info("  registered as: ${node.version} (${node.gitCommitHash})")
        logger.info("  channel: ${PolocloudVersion.CURRENT.channel} (debug: ${PolocloudVersion.CURRENT.isDebugEnabled})")
        logger.info("  build: ${PolocloudVersion.CURRENT.build}, commit: ${PolocloudVersion.CURRENT.commitId}")

        logger.info("Network:")
        logger.info("  bind address: ${config.general.bindAddress.hostname}:${config.general.bindAddress.port}")
        logger.info("  api address: ${config.general.apiAddress.hostname}:${config.general.apiAddress.port}")
        logger.info("  hostname: ${config.general.hostname}")
        logger.info("  advertised service hostname: ${config.general.serviceHostname}")
        logger.info("  cluster registration endpoint: ${config.cluster.registration.hostname}:${config.cluster.registration.port}")

        logger.info("Resources:")
        logger.info("  memory reported: ${if (node.maxMemory > 0) "${node.maxMemory}MB" else "unknown"}")
        logger.info("  memory live: ${SystemResources.usedMemory().roundToInt()}MB / ${SystemResources.maxMemory().roundToInt()}MB")
        logger.info("  cpu live: ${format(SystemResources.cpuUsage())}%")
        if (heartbeat == null) {
            logger.info("  heartbeat: none received yet")
        } else {
            logger.info("  heartbeat: ${heartbeat.heartBeatAt}")
            logger.info("    system:      ${format(heartbeat.systemCpuUsage)}% cpu, ${format(heartbeat.systemMemoryUsage)}% memory")
            logger.info("    application: ${format(heartbeat.applicationCpuUsage)}% cpu, ${format(heartbeat.applicationMemoryUsage)}% memory")
            logger.info("    tps:         ${format(heartbeat.tps)}")
        }

        logger.info("Process:")
        logger.info("  pid: ${ProcessHandle.current().pid()}")
        logger.info("  uptime: ${formatDuration(runtimeMx.uptime)}")
        logger.info("  available processors: ${Runtime.getRuntime().availableProcessors()}")
        logger.info("  jvm: ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}")
        logger.info("  os: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        logger.info("  database: ${config.localNode.database::class.simpleName}")

        logger.info("Workload:")
        logger.info("  groups (cluster-wide): ${groupService.findAll().size}")
        logger.info("  services (cluster-wide): ${serviceProvider.findAll().size}")
        logger.info("  services running here: ${localServices.size}")
        localServices.groupBy { it.groupName }.forEach { (group, services) ->
            logger.info("    $group: ${services.size}")
        }
    }

    // Locale.ROOT: a German (or other comma-decimal) system locale would otherwise turn
    // "98.7" into "98,7", which reads as a typo/thousands-separator in a CLI table.
    private fun format(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis)
        return "${duration.toHours()}h ${duration.toMinutesPart()}m ${duration.toSecondsPart()}s"
    }
}