package de.polocloud.addons.sign.system.layout

import kotlinx.serialization.Serializable

/**
 * A [BannerFrame]'s physical design — a base color plus an ordered list of pattern layers.
 *
 * @param baseColor Name of a `org.bukkit.DyeColor` (e.g. "WHITE", "RED").
 * @param patterns  Pattern layers applied on top of [baseColor], bottom to top.
 * @param hologram  Positioning/spacing of the hologram shown above the banner for this frame.
 */
@Serializable
data class BannerDesign(
    val baseColor: String,
    val patterns: List<PatternLayer> = emptyList(),
    val hologram: HologramSettings = HologramSettings(),
)

/**
 * One pattern layer of a [BannerDesign].
 *
 * @param color   Name of a `org.bukkit.DyeColor`.
 * @param pattern Name of a `org.bukkit.block.banner.PatternType`.
 */
@Serializable
data class PatternLayer(val color: String, val pattern: String)

/**
 * Where a [de.polocloud.addons.sign.system.spigot.renderer.hologram.BukkitHologram] floats
 * above its banner, and how far apart its lines are.
 *
 * @param heightOffset Blocks above the banner's block position the first line is drawn at.
 * @param lineSpacing  Blocks between each subsequent line.
 */
@Serializable
data class HologramSettings(val heightOffset: Double = 1.0, val lineSpacing: Double = 0.25)
