package dev.httpmarco.polocloud.node.cluster.node

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.database.filtering.Eq
import dev.httpmarco.polocloud.database.filtering.Filter
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.cluster.node.data.NodeHeartBeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NodeHeartBeatService(val localId: String, val factory: DatabaseConnectionFactory<*>) {

    private val logger = LoggerFactory.getLogger(NodeHeartBeatService::class.java)
    private val databaseKey = DatabaseKey("nodes_heartbeat", NodeHeartBeat::class)
    private val sysInfo = SystemInfo()
    private val processor: CentralProcessor = sysInfo.hardware.processor
    private val memory: GlobalMemory = sysInfo.hardware.memory

    // Rolling Window for TPS
    private val tickDurations = mutableListOf<Long>()

    private var schedulerJob: Job? = null

    fun startScheduler(interval: Duration = 1.seconds) {
        // before starting clean every second the old heartbeats, so we don't have old data in the database
        cleanUp()

        schedulerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                factory.executor().save(databaseKey, generate())
                delay(interval)
            }
        }
    }

    fun cleanUp() {
        val beats = factory.executor().find(databaseKey, Eq("nodeId", localId)).sortedBy { it.heartBeatAt }
        if (beats.isEmpty()) return
        val now = Clock.System.now()
        val cutoff = now - 24.hours
        val toKeep = mutableSetOf<NodeHeartBeat>()

        beats.filter { it.heartBeatAt < cutoff }
            .groupBy { (it.heartBeatAt.toEpochMilliseconds() / (10 * 60 * 1000)) }
            .forEach { (_, group) ->
                group.minByOrNull { it.heartBeatAt }?.let { toKeep.add(it) }
            }

        beats.filter { it.heartBeatAt >= cutoff }.forEach { toKeep.add(it) }

        val toDelete = beats.filter { it !in toKeep }

        logger.info(TranslationService.tr("cluster", "cluster.heartbeat.cleanup"))

        toDelete.forEach { beat ->
            factory.executor().delete(databaseKey, beat)
        }

        logger.info(TranslationService.tr("cluster", "cluster.heartbeat.cleanup.complete", "deleted" to toDelete.size, "kept" to toKeep.size))
    }


    private var prevTicks: LongArray = processor.systemCpuLoadTicks

    fun generate(): NodeHeartBeat {
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0
        prevTicks = processor.systemCpuLoadTicks // für nächsten Tick speichern

        // Memory Usage
        val usedMem = memory.total - memory.available
        val memoryUsage = (usedMem.toDouble() / memory.total) * 100.0

        // TPS
        val tickDuration = (50L..60L).random() // ms
        tickDurations.add(tickDuration)
        if (tickDurations.size > 100) tickDurations.removeAt(0)
        val avgTick = tickDurations.average()
        val tps = 1000.0 / avgTick

        return NodeHeartBeat(
            id = UUID.randomUUID().toString(),
            nodeId = localId,
            heartBeatAt = Clock.System.now(),
            cpuUsage = cpuLoad,
            memoryUsage = memoryUsage,
            tps = tps
        )
    }

}