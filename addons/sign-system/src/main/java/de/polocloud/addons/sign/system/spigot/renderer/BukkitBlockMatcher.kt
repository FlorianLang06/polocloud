package de.polocloud.addons.sign.system.spigot.renderer

import org.bukkit.Material

/**
 * Bukkit-only extension a renderer implements when its [de.polocloud.addons.sign.system.SignEntryType]
 * is detected by looking at a targeted block (a sign; a future banner block, ...).
 *
 * Kept out of the platform-agnostic [de.polocloud.addons.sign.system.SignEntryRenderer]
 * since "targeted block" is a Bukkit-specific concept — an entity-based type (e.g. a
 * painting/item-frame renderer) would implement a different, entity-based matcher
 * instead rather than force this interface's shape onto it.
 */
interface BukkitBlockMatcher {

    fun matches(material: Material): Boolean
}
