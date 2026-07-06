package de.polocloud.bridge

import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent

/**
 * Shared entry point for both proxy plugin variants (Velocity and Waterfall).
 *
 * Keeps the platform-specific plugin classes thin: they only translate the
 * platform lifecycle into a single [start] call and forward logging. All actual
 * work goes through the shipped Polocloud [api][Polocloud].
 */
class BridgeBootstrap<T>(private val instance: BridgeInstance<T>) {

    /**
     * Boots the bridge on the given [platform].
     *
     * @param platform human-readable platform name, used only for logging.
     * @param log      platform logger adapter.
     */
    fun start(platform: String, log: (String) -> Unit) {
        log("Polocloud bridge starting on $platform ...")

        // Touch the shipped API so a misconfigured classpath fails loudly and early
        // rather than on the first cloud interaction later on.
        runCatching { Polocloud.groupService }
            .onSuccess { log("Polocloud bridge ready — API linked successfully") }
            .onFailure { log("Polocloud bridge failed to initialise the API: ${it.message}") }

        Polocloud.serviceService.findAll().forEach {

            if(it.name().equals("proxy-1")) return@forEach

            instance.registerService(instance.mapService(it), it)
        }

        // Listen for cluster lifecycle events pushed by the node, so this proxy
        // (e.g. proxy-1) gets notified whenever a server starts or stops.
        Polocloud.eventService.subscribe<ServerStartedEvent> { event ->
            log("Server started in cluster: ${event.serviceName} (group: ${event.group})")
        }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { event ->
            log("Server stopped in cluster: ${event.serviceName} (group: ${event.group})")
        }
    }

    /**
     * Releases the underlying node connection. Called from the platform shutdown hook.
     */
    fun stop() {
        runCatching { Polocloud.close() }
    }
}