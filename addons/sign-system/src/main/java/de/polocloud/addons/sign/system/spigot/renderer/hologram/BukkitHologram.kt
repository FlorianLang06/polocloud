package de.polocloud.addons.sign.system.spigot.renderer.hologram

import de.polocloud.addons.sign.system.SignPosition
import de.polocloud.addons.sign.system.layout.HologramSettings
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import java.util.concurrent.ConcurrentHashMap

/**
 * A small, dependency-free hologram: one invisible, name-tagged marker [ArmorStand] per text
 * line, stacked above a [SignPosition]. No external hologram plugin is used — this addon only
 * depends on plain `spigot-api`, same as [de.polocloud.addons.sign.system.spigot.renderer.BukkitSignRenderer].
 *
 * Entities are reused across [show] calls at the same position — only their custom name is
 * updated — rather than respawned every animation tick, which would otherwise flicker every
 * 5 ticks (see [de.polocloud.addons.sign.system.SignSystem]'s animation loop). They're only
 * replaced when the number of lines actually changes.
 */
object BukkitHologram {

    private val holograms = ConcurrentHashMap<SignPosition, MutableList<ArmorStand>>()

    /** Shows/updates [lines] as a hologram above [position], spawning it on first use. */
    fun show(position: SignPosition, lines: List<String>, settings: HologramSettings) {
        if (lines.isEmpty()) {
            hide(position)
            return
        }

        val world = Bukkit.getWorld(position.world) ?: return
        val existing = holograms[position]

        val stands = if (existing != null && existing.size == lines.size) {
            existing
        } else {
            existing?.forEach { it.remove() }
            spawnStands(world, position, lines.size, settings).also { holograms[position] = it }
        }

        stands.forEachIndexed { index, stand ->
            stand.customName = lines[index]
            stand.isCustomNameVisible = true
        }
    }

    /** Removes the hologram at [position], if any. */
    fun hide(position: SignPosition) {
        holograms.remove(position)?.forEach { it.remove() }
    }

    /** Removes every hologram this platform instance owns — called on plugin disable so no armor stand outlives it. */
    fun hideAll() {
        holograms.keys.toList().forEach(::hide)
    }

    /** Spawns [count] stacked marker armor stands above [position], first line highest. */
    private fun spawnStands(world: World, position: SignPosition, count: Int, settings: HologramSettings): MutableList<ArmorStand> =
        (0 until count).map { index ->
            val y = position.y + settings.heightOffset + (count - 1 - index) * settings.lineSpacing
            val location = Location(world, position.x + 0.5, y, position.z + 0.5)
            world.spawn(location, ArmorStand::class.java) { stand ->
                stand.isVisible = false
                stand.setGravity(false)
                stand.isMarker = true
                stand.isCustomNameVisible = true
                stand.isInvulnerable = true
                // Not part of our own persisted state (SignStorage), so it must not survive
                // into the world's chunk data either — an unclean shutdown would otherwise
                // leave orphaned armor stands behind that hideAll()/hide() never learn about.
                stand.isPersistent = false
            }
        }.toMutableList()
}
