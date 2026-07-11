package de.polocloud.addons.sign.system.spigot

import de.polocloud.addons.sign.system.spigot.command.SignCommand
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitBootstrap : JavaPlugin() {

    private val platform = BukkitSignPlatform()

    override fun onEnable() {
        getCommand("signs")?.setExecutor(SignCommand(platform))
    }

    override fun onDisable() {

    }
}