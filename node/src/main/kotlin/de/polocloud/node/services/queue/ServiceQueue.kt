package de.polocloud.node.services.queue

import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.ServiceState
import de.polocloud.node.services.factory.FactoryService
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class ServiceQueue(
    private val factory: FactoryService,
    private val serviceProvider: ServiceProvider
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

    private fun tick() {
        enqueueRequired()
        processNext()
    }

    private fun enqueueRequired() {
        for (group in GroupRepository.findAll()) {
            val running = factory.runningCount(group.name)
            val queued = queue.count { it.second.name == group.name }.toLong()
            val needed = (group.minOnline - running - queued).coerceAtLeast(0)

            if (needed <= 0) continue

            logger.info(
                "Group '{}' needs {} more service(s) — minOnline: {}, running: {}, queued: {}",
                group.name, needed, group.minOnline, running, queued
            )
            repeat(needed.toInt()) {
                val index = nextIndex(group)
                val service = LocalService(Service(UUID.randomUUID(), index, group.name, ServiceState.QUEUED, "127.0.0.1", -1))

                serviceProvider.update(service)
                queue.offer(Pair(service, group))
                logger.info("Queued {}-{} [memory: {}MB, platform: {}/{}]",
                    group.name, index, group.memory, group.platform, group.version
                )
            }
        }
    }

    private fun processNext() {
        val (service, group) = queue.poll() ?: return
        logger.info("Starting {}-{} [memory: {}MB, platform: {}/{}]", group.name, service.index, group.memory, group.platform, group.version)
        try {
            factory.start(service, group)
        } catch (e: Exception) {
            logger.error("Failed to start {}-{}: {}", group.name, service.index, e.message)
        }
    }

    private fun nextIndex(group: Group): Int {
        val usedIndexes = queue
            .filter { it.second.name == group.name }
            .map { it.first.index }
            .toSet() + factory.runningIndexes(group.name)
        var index = 1
        while (index in usedIndexes) index++
        return index
    }
}
