package de.polocloud.addons.sign.system.spigot.renderer

import de.polocloud.addons.sign.system.SignEntry
import de.polocloud.addons.sign.system.SignEntryRenderer
import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.addons.sign.system.layout.LayoutFrame
import de.polocloud.addons.sign.system.layout.SignFrame
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side

/** Renders [SignEntryType.SIGN] entries onto a real wooden/wall sign block. */
class BukkitSignRenderer : SignEntryRenderer(SignEntryType.SIGN), BukkitBlockMatcher {

    override fun matches(material: Material): Boolean = material.name.endsWith("_SIGN")

    override fun render(entry: SignEntry, frame: LayoutFrame) {
        val signFrame = frame as? SignFrame ?: return
        val sign = entry.position.blockStateAt<Sign>() ?: return

        signFrame.lines.forEachIndexed { index, line ->
            sign.getSide(Side.FRONT).setLine(index, line)
        }

        signFrame.backgroundBlock?.let { material ->
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

                else -> return@let
            }

            behind.type = Material.valueOf(material)
        }

        sign.update()
    }

    override fun remove(entry: SignEntry) {
        val sign = entry.position.blockStateAt<Sign>() ?: return

        repeat(4) { sign.getSide(Side.FRONT).setLine(it, "") }
        sign.update()
    }
}
