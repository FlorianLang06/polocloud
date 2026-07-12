package de.polocloud.addons.sign.system.spigot.renderer

import de.polocloud.addons.sign.system.SignEntry
import de.polocloud.addons.sign.system.SignEntryRenderer
import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.addons.sign.system.layout.LayoutFrame
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side

/** Renders [SignEntryType.SIGN] entries onto a real wooden/wall sign block. */
class BukkitSignRenderer : SignEntryRenderer(SignEntryType.SIGN), BukkitBlockMatcher {

    override fun matches(material: Material): Boolean = material.name.endsWith("_SIGN")

    override fun render(entry: SignEntry, frame: LayoutFrame) {
        val sign = signAt(entry) ?: return

        frame.lines.forEachIndexed { index, line ->
            sign.getSide(Side.FRONT).setLine(index, line)
        }

        if(frame.backgroundBlock != null) {
            val block = sign.block

            val behind = when (block.blockData) {
                is org.bukkit.block.data.type.WallSign -> {
                    val data = block.blockData as org.bukkit.block.data.type.WallSign
                    block.getRelative(data.facing.oppositeFace)
                }

                is org.bukkit.block.data.type.Sign -> {
                    val data = block.blockData as org.bukkit.block.data.type.Sign
                    block.getRelative(data.rotation.oppositeFace)
                }

                else -> return
            }

            behind.type = Material.valueOf(frame.backgroundBlock!!)
        }

        sign.update()
    }

    override fun remove(entry: SignEntry) {
        val sign = signAt(entry) ?: return

        repeat(4) { sign.getSide(Side.FRONT).setLine(it, "") }
        sign.update()
    }

    private fun signAt(entry: SignEntry): Sign? {
        val world = Bukkit.getWorld(entry.position.world) ?: return null
        val block = world.getBlockAt(entry.position.x, entry.position.y, entry.position.z)
        return block.state as? Sign
    }
}
