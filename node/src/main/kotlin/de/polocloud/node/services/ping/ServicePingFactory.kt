package de.polocloud.node.services.ping

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceEventMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.shared.service.ServiceState
import de.polocloud.shared.event.server.PlayerCountChangedEvent
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
 * Once a service is `RUNNING`, it keeps being pinged — at a much lower cadence — purely
 * to refresh [LocalService.onlinePlayers] / [LocalService.maxPlayers]. That keeps the
 * player count live for as long as the service runs, without the per-service poll cost
 * scaling past what a handful of pings per second can handle: starting services are
 * checked every tick (state changes should be picked up fast), running services only
 * every [PLAYER_POLL_INTERVAL_MILLIS]. Stopping/stopped services are skipped entirely.
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
        val now = System.currentTimeMillis()
        for (service in serviceProvider.localServices) {
            if (service.process?.isAlive != true) continue
            if (service.port <= 0) continue

            when {
                isAwaitingOnline(service) -> pingStarting(service)
                service.state == ServiceState.RUNNING -> pingPlayerCount(service, now)
            }
        }
    }

    // Pinged over loopback: the pinger is co-located with the service on this node, so it
    // never depends on the (possibly public) advertised hostname being reachable from here.

    private fun pingStarting(service: LocalService) {
        val result = MinecraftServerPing.ping(PING_HOST, service.port) ?: return
        markOnline(service, result)
    }

    /**
     * Refreshes [LocalService.onlinePlayers] / [LocalService.maxPlayers] for a service that
     * is already running, at most once every [PLAYER_POLL_INTERVAL_MILLIS]. A failed ping
     * (e.g. a momentary hiccup) leaves the last known counts in place rather than resetting
     * them to zero — the service's actual `RUNNING`/stopped state is tracked elsewhere.
     *
     * Fires [PlayerCountChangedEvent] only when a count actually differs from what was last
     * reported, so a sign/monitor system can react live without itself polling every
     * service on an interval — and the event bus isn't flooded once per service every
     * [PLAYER_POLL_INTERVAL_MILLIS] regardless of whether anything changed.
     */
    private fun pingPlayerCount(service: LocalService, now: Long) {
        if (now - service.lastPlayerPollAt < PLAYER_POLL_INTERVAL_MILLIS) return
        service.lastPlayerPollAt = now

        val result = MinecraftServerPing.ping(PING_HOST, service.port) ?: return
        val changed = service.onlinePlayers != result.onlinePlayers || service.maxPlayers != result.maxPlayers
        service.onlinePlayers = result.onlinePlayers
        service.maxPlayers = result.maxPlayers
        service.motd = result.description

        if (changed) {
            ClusterEventService.call(PlayerCountChangedEvent(ServiceEventMapper.toShared(service)))
        }
    }

    /** True while a service has been started but has not yet been confirmed online. */
    private fun isAwaitingOnline(service: LocalService): Boolean =
        service.state == ServiceState.STARTING || service.state == ServiceState.QUEUED

    private fun markOnline(service: LocalService, result: MinecraftPingResult) {
        service.state = ServiceState.RUNNING
        service.onlinePlayers = result.onlinePlayers
        service.maxPlayers = result.maxPlayers
        service.motd = result.description
        service.lastPlayerPollAt = System.currentTimeMillis()
        // Persist the RUNNING transition so the database no longer shows the service as
        // STARTING once it is actually online.
        serviceProvider.persist(service)
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

        /** How often an already-`RUNNING` service is re-pinged to refresh its player count. */
        const val PLAYER_POLL_INTERVAL_MILLIS = 5000L

        /** Loopback host used to reach co-located services from the node's own ping thread. */
        const val PING_HOST = "127.0.0.1"
    }
}