package de.polocloud.node.services.queue

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.NodePeerServiceQuery
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.proto.NodeState
import de.polocloud.shared.service.ServiceState
import de.polocloud.node.services.factory.FactoryService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class ServiceQueue(
    private val factory: FactoryService,
    private val serviceProvider: ServiceProvider,
    // Injectable so the round-robin/eligibility logic can be unit-tested without a
    // real database — default to the real repositories in production.
    private val groups: () -> List<Group> = { GroupRepository.findAll() },
    private val onlineNodes: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val peerQuery: PeerServiceQuery = NodePeerServiceQuery(),
) {

    private lateinit var thread: Thread
    private val logger = LoggerFactory.getLogger(ServiceQueue::class.java)
    private val queue: Queue<Pair<LocalService, Group>> = LinkedList()

    fun run() {
        thread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tick()
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    // Interrupted by close() during shutdown — exit the loop quietly.
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error("Service queue tick failed", e)
                }
            }
        }, "service-queue")
        thread.isDaemon = true
        thread.start()
        logger.info("Service queue started")
    }

    fun close() {
        thread.interrupt()
    }

    /**
     * Drops every still-queued service of [groupName] (e.g. when the group is deleted),
     * so no new services of a removed group get started.
     *
     * Synchronised on the queue because it is invoked from the terminal/gRPC thread while
     * the queue thread also mutates the list.
     */
    fun removeGroup(groupName: String) {
        synchronized(queue) {
            queue.removeIf { it.second.name.equals(groupName, ignoreCase = true) }
        }
    }

    private fun tick() {
        enqueueRequired()
        drainQueue()
    }

    /**
     * Tops each group up to its cluster-wide `minOnline`, restricted to the nodes it is
     * allowed to run on ([GroupNodeEligibility]).
     *
     * `minOnline` is a cluster-wide target, not per-node: every eligible online node
     * independently computes the same eligible-node list and the same cluster-wide
     * running count (via [clusterState], which fans out to peers exactly like
     * [de.polocloud.node.communication.handler.services.ListServicesServerHandler]
     * does), then deterministically round-robins the outstanding deficit across that
     * list so only one node claims each missing replica. No locks or leader RPC are
     * involved — like head election and the heartbeat monitor, this is best-effort and
     * self-healing rather than transactionally exact; a brief over/under-shoot is
     * possible only in the sub-second window between two nodes ticking concurrently.
     */
    /** The groups and indexes currently queued, e.g. `lobby-1`. Exposed for testing. */
    internal fun queuedIndexes(groupName: String): List<Int> =
        queue.filter { it.second.name == groupName }.map { it.first.index }

    /** Runs a single `enqueueRequired` pass without starting the background thread. Exposed for testing. */
    internal fun enqueueRequiredForTest() = enqueueRequired()

    private fun enqueueRequired() {
        for (group in groups()) {
            val eligible = GroupNodeEligibility.eligibleOnlineNodes(group, onlineNodes()).sortedBy { it.id.toString() }
            if (eligible.isEmpty()) continue

            val selfPosition = eligible.indexOfFirst { it.id.toString() == serviceProvider.nodeId }
            // This node isn't (or is no longer) allowed to run this group — leave it to
            // whichever node(s) are actually eligible.
            if (selfPosition < 0) continue

            val cluster = clusterState(group, eligible)
            val queued = queue.count { it.second.name == group.name }.toLong()
            val clusterNeeded = (group.minOnline - cluster.running - queued).coerceAtLeast(0)

            if (clusterNeeded <= 0) continue

            val myShare = (0 until clusterNeeded.toInt()).count { k ->
                (cluster.running.toInt() + k) % eligible.size == selfPosition
            }
            if (myShare <= 0) continue

            logger.info(
                "Group '{}' needs {} more service(s) cluster-wide (this node: {}) — minOnline: {}, cluster running: {}, queued: {}",
                group.name, clusterNeeded, myShare, group.minOnline, cluster.running, queued
            )
            repeat(myShare) {
                val index = nextIndex(group, cluster.usedIndexes)
                val service = LocalService(Service(UUID.randomUUID(), index, group.name, ServiceState.QUEUED, "127.0.0.1", -1))

                serviceProvider.update(service)
                queue.offer(Pair(service, group))
                logger.info("Queued {}-{} [memory: {}MB, platform: {}/{}]",
                    group.name, index, group.memory, group.platform, group.version
                )
            }
        }
    }

    private data class ClusterState(val running: Long, val usedIndexes: Set<Int>)

    /**
     * Aggregates [group]'s running-service count and used indexes across every node in
     * [eligible] besides this one, via the same peer-query mechanism the cluster-wide
     * service listing handlers use ([de.polocloud.node.services.cluster.PeerServiceQuery]).
     * A slow or unreachable peer is skipped rather than failing the whole tick, at the
     * cost of briefly under-counting that peer's services.
     */
    private fun clusterState(group: Group, eligible: List<NodeData>): ClusterState {
        val localRunning = factory.runningCount(group.name)
        val localIndexes = factory.runningIndexes(group.name)

        val others = eligible.filter { it.id.toString() != serviceProvider.nodeId }
        if (others.isEmpty()) return ClusterState(localRunning, localIndexes)

        val remote = runBlocking {
            coroutineScope {
                others.map { node ->
                    async {
                        runCatching { peerQuery.localServicesOf(node, group.name) }
                            .onFailure { logger.warn("Failed to query services from node {}: {}", node.name(), it.message) }
                            .getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }

        return ClusterState(
            localRunning + remote.size,
            localIndexes + remote.map { it.index }.toSet(),
        )
    }

    /**
     * Starts every service currently queued, back-to-back, instead of throttling to one
     * per [run] tick. A single service typically starts in well under a second once its
     * platform JAR is cached, so waiting a full tick interval between each one only adds
     * dead time when several services are queued at once (e.g. `minOnline > 1`, or many
     * groups needing services after a node restart).
     *
     * One service failing to start must not block the rest of the batch.
     */
    private fun drainQueue() {
        while (true) {
            val (service, group) = queue.poll() ?: return
            logger.info("Starting {}-{} [memory: {}MB, platform: {}/{}]", group.name, service.index, group.memory, group.platform, group.version)
            try {
                factory.start(service, group)
            } catch (e: Exception) {
                logger.error("Failed to start {}-{}: {}", group.name, service.index, e.message)
            }
        }
    }

    /** [clusterOtherIndexes] are indexes already used by eligible peers, so two nodes never pick the same one. */
    private fun nextIndex(group: Group, clusterOtherIndexes: Set<Int>): Int {
        val usedIndexes = queue
            .filter { it.second.name == group.name }
            .map { it.first.index }
            .toSet() + clusterOtherIndexes
        var index = 1
        while (index in usedIndexes) index++
        return index
    }
}
