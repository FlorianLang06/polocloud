package de.polocloud.addons.sign.system.spigot

import de.polocloud.addons.sign.system.SignData
import de.polocloud.addons.sign.system.SignPlatform
import de.polocloud.addons.sign.system.layout.SignLayout
import de.polocloud.shared.service.ServiceState
import org.bukkit.Material
import org.bukkit.block.sign.Side

class BukkitSignPlatform : SignPlatform() {

    override fun listSignTypes(): Set<String> {
        return setOf(Material.OAK_WALL_SIGN.toString())
    }

    override fun displaySign(data: SignData) {
        val world = org.bukkit.Bukkit.getWorld(data.position.world) ?: return
        val block = world.getBlockAt(data.position.x, data.position.y, data.position.z)

        val signState = block.state as? org.bukkit.block.Sign ?: return
        val signLayout = data.layout as SignLayout

        signLayout.frames[ServiceState.UNKNOWN]?.get(0)?.lines?.forEachIndexed { index, line ->
            signState.getSide(Side.FRONT).setLine(index, line)
        }

        signState.update()
    }
}