package de.polocloud.addons.sign.system.spigot

import de.polocloud.addons.sign.system.SignSystem
import de.polocloud.addons.sign.system.spigot.command.SignCommand
import org.bukkit.plugin.java.JavaPlugin

class BukkitBootstrap : JavaPlugin() {

    private lateinit var platform: BukkitSignPlatform
    private lateinit var signSystem: SignSystem

    override fun onEnable() {
        platform = BukkitSignPlatform(this)
        signSystem = SignSystem(platform)
        signSystem.start()

        getCommand("signs")?.setExecutor(SignCommand(signSystem, platform))
    }

    override fun onDisable() {
        signSystem.stop()
    }
}