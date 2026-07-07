package de.polocloud.node.services.ping

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceEventMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.ServiceState
import de.polocloud.shared.event.server.ServerStartedEvent
import org.slf4j.LoggerFactory

/**
 * Watches the node's local services and promotes them to [ServiceState.RUNNING] once
 * they answer a Minecraft Server List Ping.
 *
 * A freshly launched service is [ServiceState.STARTING]: the process is alive but the
 * server is still loading worlds/plugins and not yet accepting connections. This factory
 * polls those services on a background thread and, as soon as one responds to a ping,
 * marks it online (`RUNNING`) and evaluates the returned status (version, player counts,
 * MOTD).
 *
 * Only services in a pre-online state are pinged; already `RUNNING` or stopping/stopped
 * services are skipped so the poll cost scales with the number of *starting* services,
 * not the whole cluster.
 */
class ServicePingFactory(private val serviceProvider: ServiceProvider) {

    private val logger = LoggerFactory.getLogger(ServicePingFactory::class.java)
    private lateinit var thread: Thread

    fun run() {
        thread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tick()
                    Thread.sleep(POLL_INTERVAL_MILLIS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error("Service ping tick failed", e)
                }
            }
        }, "service-ping")
        thread.isDaemon = true
        thread.start()
        logger.info("Service ping factory started")
    }

    fun close() {
        if (this::thread.isInitialized) thread.interrupt()
    }

    private fun tick() {
        for (service in serviceProvider.localServices) {
            if (!isAwaitingOnline(service)) continue
            if (service.process?.isAlive != true) continue
            if (service.port <= 0) continue

            val result = MinecraftServerPing.ping(service.host, service.port) ?: continue
            markOnline(service, result)
        }
    }

    /** True while a service has been started but has not yet been confirmed online. */
    private fun isAwaitingOnline(service: LocalService): Boolean =
        service.state == ServiceState.STARTING || service.state == ServiceState.QUEUED

    private fun markOnline(service: LocalService, result: MinecraftPingResult) {
        service.state = ServiceState.RUNNING
        logger.info(
            "Service {} is ONLINE — {} (protocol {}), {}/{} players, {}ms",
            service.name(), result.versionName, result.protocol,
            result.onlinePlayers, result.maxPlayers, result.latencyMillis,
        )

        // Re-broadcast the started event now that the service is reachable, so
        // subscribers see it with its confirmed RUNNING state.
        ClusterEventService.call(ServerStartedEvent(ServiceEventMapper.toShared(service)))
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1000L
    }
}