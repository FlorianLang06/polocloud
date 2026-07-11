package de.polocloud.addons.sign.system.spigot

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.addons.sign.system.SignPlatform
import de.polocloud.addons.sign.system.spigot.renderer.BukkitBlockMatcher
import de.polocloud.addons.sign.system.spigot.renderer.BukkitSignRenderer
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path

/**
 * Bukkit implementation of [SignPlatform]. Registers one [de.polocloud.addons.sign.system.SignEntryRenderer]
 * per supported [SignEntryType] — currently just [SignEntryType.SIGN]; a painting or
 * banner renderer registers here the same way once it exists.
 */
class BukkitSignPlatform(private val plugin: JavaPlugin) : SignPlatform() {

    override val dataDirectory: Path = plugin.dataFolder.toPath()

    init {
        register(BukkitSignRenderer())
    }

    override fun scheduleRepeating(intervalTicks: Long, task: () -> Unit) {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable(task), intervalTicks, intervalTicks)
    }

    /** The [SignEntryType] whose block-matcher renderer accepts [material], if any — used by `/signs add`. */
    fun detectType(material: Material): SignEntryType? =
        renderers().firstOrNull { it is BukkitBlockMatcher && it.matches(material) }?.type
}