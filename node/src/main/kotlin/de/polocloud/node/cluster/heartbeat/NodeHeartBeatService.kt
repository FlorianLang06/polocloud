package de.polocloud.node.cluster.heartbeat

import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trError
import de.polocloud.node.core.environment.NodeEnvironment
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Service for generating and managing heartbeats for a cluster node.
 *
 * This service collects system metrics like CPU and memory usage, calculates TPS (Ticks per Second),
 * and stores this information periodically in the database. Additionally, old heartbeats are cleaned up.
 *
 * @property factory The database connection used for saving heartbeats.
 */
class NodeHeartBeatService {

    private val logger = LoggerFactory.getLogger(NodeHeartBeatService::class.java)
    private val sysInfo = SystemInfo()
    private val processor: CentralProcessor = sysInfo.hardware.processor
    private val memory: GlobalMemory = sysInfo.hardware.memory

    private val tickDurations = mutableListOf<Long>()
    private var schedulerJob: Job? = null
    private var prevTicks: LongArray = processor.systemCpuLoadTicks

    /**
     * Starts the heartbeat scheduler.
     *
     * @param interval The interval between heartbeats (default: 1 second).
     */
    fun startScheduler(interval: Duration = 1.seconds) {
        cleanUp() // Clean old heartbeats before starting

        val nodeId = NodeEnvironment.runtime.nodeId.get()

        schedulerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                runCatching {
                    NodeHeartBeatRepository.save(generate())
                }.onFailure { exception ->
                    logger.trError("cluster", "cluster.heartbeat.save_failed", exception, "nodeId" to nodeId)
                }
                delay(interval)
            }
        }
    }

    /**
     * Stops the heartbeat scheduler if it is running.
     */
    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    /**
     * Cleans up old heartbeats in the database.
     *
     * Heartbeats younger than 24 hours are kept, as well as at least one heartbeat per 10 minutes
     * in older data.
     */
    fun cleanUp() {
        val beats = NodeHeartBeatRepository.find(NodeEnvironment.runtime.nodeId.get()).sortedBy { it.heartBeatAt }
        if (beats.isEmpty()) return

        val now = Clock.System.now()
        val cutoff = now - 24.hours
        val toKeep = mutableSetOf<NodeHeartBeat>()

        // Keep at least one heartbeat per 10 minutes for older data
        beats.filter { it.heartBeatAt < cutoff }
            .groupBy { it.heartBeatAt.toEpochMilliseconds() / (10 * 60 * 1000) }
            .forEach { (_, group) ->
                group.minByOrNull { it.heartBeatAt }?.let { toKeep.add(it) }
            }

        // Keep all recent heartbeats
        beats.filter { it.heartBeatAt >= cutoff }.forEach { toKeep.add(it) }

        val toDelete = beats.filter { it !in toKeep }

        logger.info(TranslationService.tr("cluster", "cluster.heartbeat.cleanup"))

        toDelete.forEach { beat ->
            NodeHeartBeatRepository.delete(beat)
        }

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.heartbeat.cleanup.complete",
                "deleted" to toDelete.size,
                "kept" to toKeep.size
            )
        )
    }

    /**
     * Generates a new heartbeat based on current system metrics.
     *
     * @return A [NodeHeartBeat] object with CPU usage, memory usage, and TPS data.
     */
    fun generate(): NodeHeartBeat {
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0
        prevTicks = processor.systemCpuLoadTicks

        val usedMem = memory.total - memory.available
        val memoryUsage = (usedMem.toDouble() / memory.total) * 100.0

        // TPS calculation (simulated tick durations)
        val tickDuration = (50L..60L).random()
        tickDurations.add(tickDuration)
        if (tickDurations.size > 100) tickDurations.removeAt(0)
        val avgTick = tickDurations.average()
        val tps = 1000.0 / avgTick

        return NodeHeartBeat(
            id = UUID.randomUUID().toString(),
            nodeId = NodeEnvironment.runtime.nodeId.get(),
            heartBeatAt = Clock.System.now(),
            cpuUsage = cpuLoad,
            memoryUsage = memoryUsage,
            tps = tps
        )
    }
}