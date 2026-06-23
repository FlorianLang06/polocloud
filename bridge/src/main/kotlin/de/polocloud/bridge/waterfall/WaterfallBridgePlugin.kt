package de.polocloud.bridge.waterfall

import de.polocloud.bridge.BridgeBootstrap
import net.md_5.bungee.api.plugin.Plugin

/**
 * Waterfall (BungeeCord) entry point for the Polocloud bridge.
 *
 * The runtime plugin description is provided by `bungee.yml`.
 */
class WaterfallBridgePlugin : Plugin() {

    override fun onEnable() {
        BridgeBootstrap.start("Waterfall") { logger.info(it) }
    }

    override fun onDisable() {
        BridgeBootstrap.stop()
    }
}