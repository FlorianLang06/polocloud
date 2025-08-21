package dev.httpmarco.polocloud.bridges.waterdog

import dev.httpmarco.polocloud.bridge.api.BridgeInstance
import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.events.definitions.PlayerJoinEvent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerLeaveEvent
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import dev.httpmarco.polocloud.shared.service.Service
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.event.defaults.InitialServerConnectedEvent
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent
import dev.waterdog.waterdogpe.event.defaults.ServerConnectedEvent
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import java.net.InetSocketAddress

class WaterdogBridgeInstance : BridgeInstance<BedrockServerInfo>() {

    val registeredFallbacks = ArrayList<BedrockServerInfo>()

    init {
        registerEvents()
        initialize()
    }

    override fun generateInfo(service: Service): BedrockServerInfo {
        return BedrockServerInfo(service.name(), InetSocketAddress(service.hostname, service.port), null)
    }

    override fun registerService(
        identifier: BedrockServerInfo,
        fallback: Boolean
    ) {
        ProxyServer.getInstance().registerServerInfo(identifier)

        if (fallback) {
            registeredFallbacks.add(identifier)
        }
    }

    override fun unregisterService(identifier: BedrockServerInfo) {
        ProxyServer.getInstance().removeServerInfo(identifier.serverName)
        registeredFallbacks.remove(identifier)
    }

    override fun findInfo(name: String): BedrockServerInfo? {
        return ProxyServer.getInstance().getServerInfo(name) as BedrockServerInfo?
    }

    private fun registerEvents() {
        val eventManager = ProxyServer.getInstance().eventManager

        eventManager.subscribe(PlayerLoginEvent::class.java) { event ->
            val player = event.player
            val fallback = findFallback()

            if (fallback == null) {
                event.cancelReason = "No fallback servers are registered."
                event.isCancelled = true
                ProxyServer.getInstance().logger.warning("[LoginEvent] Kein Fallback verfügbar für ${player.name}")
            } else {
                ProxyServer.getInstance().logger.info("[LoginEvent] Spieler darf einloggen, Fallback vorhanden")
                // NICHT redirectServer hier aufrufen!
            }
        }

        eventManager.subscribe(InitialServerConnectedEvent::class.java) { event ->
            val player = event.player

            // Spieler hat noch keinen Server? Dann Fallback setzen
            if (player.connectingServer == null) {
                val fallback = findFallback()
                if (fallback != null) {
                    player.redirectServer(fallback)
                    ProxyServer.getInstance().logger.info("[InitialServerConnected] Spieler ${player.name} wird zu Fallback geleitet: ${fallback.serverName}")
                } else {
                    player.disconnect("No fallback servers available.")
                }
            }

            val cloudPlayer = PolocloudPlayer(player.name, player.uniqueId, event.serverInfo.serverName)
            updatePolocloudPlayer(PlayerJoinEvent(cloudPlayer))
        }

        eventManager.subscribe(PlayerDisconnectedEvent::class.java) { event ->
            val player = event.player
            val serverName = player.connectingServer?.serverName ?: "unknown"
            val cloudPlayer = PolocloudPlayer(player.name, player.uniqueId, serverName)
            updatePolocloudPlayer(PlayerLeaveEvent(cloudPlayer))
        }
    }

    fun findFallback(): ServerInfo? {
        return registeredFallbacks.minByOrNull { it.players.size }
    }
}