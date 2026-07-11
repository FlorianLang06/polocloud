package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.node.cluster.heartbeat.NodeHeartBeat
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.types.NodeArgument
import de.polocloud.proto.NodeState
import org.slf4j.LoggerFactory
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Terminal command giving an overview of the cluster: known nodes, their live state and
 * resource load, the current head, and cluster-wide group/service totals.
 *
 * `cluster` (no args) prints a summary, `cluster list` lists every node one-line each,
 * and `cluster <name>` shows the detailed view of a single node — the same three-tier
 * shape as [GroupCommand] (list/info) and [ServiceCommand] (list/bare-argument info).
 */
class ClusterCommand(
    private val localNodeContainer: LocalNodeContainer,
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
) : Command("cluster", "View the state of the cluster and its nodes") {

    private val logger = LoggerFactory.getLogger(ClusterCommand::class.java)

    init {
        val nodeArgument = NodeArgument("name")

        defaultExecution { overview() }

        syntax({
            list()
        }, "List all nodes in the cluster", KeywordArgument("list"))

        syntax({
            info(it.arg(nodeArgument))
        }, "Show detailed information about a node", nodeArgument)
    }

    private fun overview() {
        val nodes = NodeRepository.findAll()
        if (nodes.isEmpty()) {
            logger.info("The cluster has no known nodes.")
            return
        }

        val byState = nodes.groupingBy { it.state }.eachCount()
        val head = nodes.firstOrNull { it.head }

        val onlineNodes = nodes.filter { it.state == NodeState.ONLINE }
        val totalMemory = onlineNodes.sumOf { it.maxMemory }
        val usedMemory = onlineNodes.sumOf { node ->
            latestHeartbeat(node)?.let { (it.systemMemoryUsage / 100.0) * node.maxMemory } ?: 0.0
        }

        logger.info("Cluster overview:")
        logger.info(
            "  nodes: ${nodes.size} total (${byState.entries.joinToString { (state, count) -> "$count ${state.name.lowercase()}" }})"
        )
        logger.info("  head: ${head?.name() ?: "(none — election pending)"}")
        logger.info("  groups: ${groupService.findAll().size}")
        logger.info("  services: ${serviceProvider.findAll().size}")
        logger.info(
            if (totalMemory > 0)
                "  memory: ${usedMemory.roundToInt()}MB / ${totalMemory}MB used across online nodes"
            else
                "  memory: unknown (no online node reports its capacity)"
        )
        logger.info("  this node: ${localNodeContainer.data.name()}${if (localNodeContainer.data.head) " (head)" else ""}")
        logger.info("Use 'cluster list' to see all nodes, or 'cluster <name>' for details.")
    }

    private fun list() {
        val nodes = NodeRepository.findAll()
        if (nodes.isEmpty()) {
            logger.info("There are no nodes.")
            return
        }
        logger.info("Nodes (${nodes.size}):")
        nodes.sortedBy { it.index }.forEach { node ->
            val load = latestHeartbeat(node)?.let {
                " | load: ${format(it.systemCpuUsage)}% cpu, ${format(it.systemMemoryUsage)}% mem, ${format(it.tps)} tps"
            } ?: " | load: -"
            logger.info(
                "  ${node.name()}${if (node.head) " (head)" else ""} | state: ${node.state} | host: ${node.hostname}:${node.port}$load"
            )
        }
    }

    private fun info(node: NodeData) {
        val heartbeat = latestHeartbeat(node)
        logger.info("Node ${node.name()}:")
        logger.info("  id: ${node.id}")
        logger.info("  state: ${node.state}")
        logger.info("  head: ${if (node.head) "yes (since ${node.electedAt})" else "no"}")
        logger.info("  host: ${node.hostname}:${node.port}")
        logger.info("  version: ${node.version} (${node.gitCommitHash})")
        logger.info("  memory: ${if (node.maxMemory > 0) "${node.maxMemory}MB" else "unknown"}")
        logger.info("  first connection: ${node.firstConnection}")
        logger.info("  last connection: ${node.lastConnection}")
        if (heartbeat == null) {
            logger.info("  heartbeat: none received yet")
        } else {
            logger.info("  heartbeat: ${heartbeat.heartBeatAt}")
            logger.info("    system:      ${format(heartbeat.systemCpuUsage)}% cpu, ${format(heartbeat.systemMemoryUsage)}% memory")
            logger.info("    application: ${format(heartbeat.applicationCpuUsage)}% cpu, ${format(heartbeat.applicationMemoryUsage)}% memory")
            logger.info("    tps:         ${format(heartbeat.tps)}")
        }
    }

    private fun latestHeartbeat(node: NodeData): NodeHeartBeat? =
        NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }

    // Locale.ROOT: a German (or other comma-decimal) system locale would otherwise turn
    // "98.7" into "98,7", which reads as a typo/thousands-separator in a CLI table.
    private fun format(value: Double): String = String.format(Locale.ROOT, "%.1f", value)
}
