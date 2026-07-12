package de.polocloud.bridge.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import de.polocloud.shared.service.Service
import de.polocloud.bridge.BridgeBootstrap
import de.polocloud.bridge.BridgeInstance
import org.slf4j.Logger
import java.net.InetSocketAddress

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
) : BridgeInstance<ServerInfo>() {

    private val bootstrap = BridgeBootstrap(this)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        // remove all registered servers on startup
        server.allServers.forEach {
            server.unregisterServer(it.serverInfo)
        }

        bootstrap.start("Velocity") { logger.info(it) }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        bootstrap.stop()
    }

    @Subscribe
    fun onConnect(event: PlayerChooseInitialServerEvent) {
        // A player may only join through this proxy if a fallback group has a running
        // service. If none is found, the initial server is left unset and Velocity
        // disconnects the player instead of dropping them onto an arbitrary server.
        bootstrap.bestFallback()
            ?.let { service -> server.getServer(service.name()).orElse(null) }
            ?.let { event.setInitialServer(it) }
    }

    @Subscribe
    fun onKick(event: KickedFromServerEvent) {
        // Send a kicked player to the emptiest fallback (by priority) instead of just
        // disconnecting them, excluding the server that just kicked them.
        val target = bootstrap.bestFallback(excludeServiceName = event.server.serverInfo.name)
            ?.let { service -> server.getServer(service.name()).orElse(null) }
            ?: return

        event.result = KickedFromServerEvent.RedirectPlayer.create(target)
    }

    override fun registerService(
        info: ServerInfo,
        service: Service
    ) {
        logger.info("Registering service ${service.name()} on ${info.address}")
        server.registerServer(info)
    }

    override fun unregisterService(
        info: ServerInfo,
        service: Service
    ) {
        logger.info("Unregistering service ${service.name()} on ${info.address}")
        server.unregisterServer(info)
    }

    override fun mapService(service: Service): ServerInfo {
        return ServerInfo(service.name(), InetSocketAddress(service.host, service.port))
    }
}