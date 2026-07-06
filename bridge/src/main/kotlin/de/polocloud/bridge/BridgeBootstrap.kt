package de.polocloud.bridge

import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.service.Service

/**
 * Shared entry point for both proxy plugin variants (Velocity and Waterfall).
 *
 * Keeps the platform-specific plugin classes thin: they only translate the
 * platform lifecycle into a single [start] call and forward logging. All actual
 * work goes through the shipped Polocloud [api][Polocloud].
 */
class BridgeBootstrap<T>(private val instance: BridgeInstance<T>) {

    private companion object {
        // The proxy must not register itself as one of its own backend servers.
        // TODO: derive this from the proxy's own identity/group type instead of a
        //       hard-coded name so multiple proxies don't register each other.
        const val OWN_SERVICE_NAME = "proxy-1"
    }

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

        // Register everything already running when this proxy boots.
        Polocloud.serviceService.findAll().forEach(::registerIfEligible)

        // Then keep the registry in sync: the node pushes a lifecycle event whenever a
        // server starts or stops, so services that come and go after boot are added and
        // removed on the proxy instead of only reflecting the boot-time snapshot.
        Polocloud.eventService.subscribe<ServerStartedEvent> { event ->
            log("Server started in cluster: ${event.service.name()} (group: ${event.service.group})")
            registerIfEligible(event.service)
        }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { event ->
            val service = event.service
            log("Server stopped in cluster: ${service.name()} (group: ${service.group})")
            instance.unregisterService(instance.mapService(service), service)
        }
    }

    /** Registers [service] on this proxy unless it is the proxy's own instance. */
    private fun registerIfEligible(service: Service) {
        if (service.name().equals(OWN_SERVICE_NAME, ignoreCase = true)) return
        instance.registerService(instance.mapService(service), service)
    }

    /**
     * Releases the underlying node connection. Called from the platform shutdown hook.
     */
    fun stop() {
        runCatching { Polocloud.close() }
    }
}