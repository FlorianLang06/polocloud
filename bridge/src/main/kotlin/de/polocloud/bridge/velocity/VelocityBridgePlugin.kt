package de.polocloud.bridge.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import de.polocloud.bridge.BridgeBootstrap
import org.slf4j.Logger

/**
 * Velocity entry point for the Polocloud bridge.
 *
 * The runtime plugin description is provided by `velocity-plugin.json`; the
 * [Plugin] annotation only documents the metadata here.
 */
@Plugin(
    id = "polocloud-bridge",
    name = "Polocloud Bridge",
    version = "3.0.0",
    description = "Connects this proxy to the Polocloud node.",
    authors = ["polocloud"],
)
class VelocityBridgePlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
) {

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        BridgeBootstrap.start("Velocity") { logger.info(it) }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        BridgeBootstrap.stop()
    }
}