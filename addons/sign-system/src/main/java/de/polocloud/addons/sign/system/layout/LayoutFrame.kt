package de.polocloud.addons.sign.system.layout

import kotlinx.serialization.Serializable

/**
 * One still image of a [StateAnimation] — what's shown while it's this frame's turn.
 *
 * Sealed so each [de.polocloud.addons.sign.system.SignEntryType] gets its own frame shape
 * instead of one class carrying fields only some types use: [SignFrame] and [BannerFrame]
 * are the only two kinds, and each renderer only ever looks at its own kind (see
 * [de.polocloud.addons.sign.system.spigot.renderer.BukkitSignRenderer]/
 * [de.polocloud.addons.sign.system.spigot.renderer.BukkitBannerRenderer]).
 */
@Serializable
sealed class LayoutFrame {
    abstract val lines: List<String>
}

/** A [de.polocloud.addons.sign.system.SignEntryType.SIGN] frame: text lines plus an optional block placed behind the sign. */
@Serializable
data class SignFrame(
    override val lines: List<String>,
    val backgroundBlock: String? = null,
) : LayoutFrame()

/**
 * A [de.polocloud.addons.sign.system.SignEntryType.BANNER] frame. Banners can't display text
 * themselves, so [lines] instead drives the hologram floating above the block
 * ([de.polocloud.addons.sign.system.spigot.renderer.hologram.BukkitHologram]); [design]
 * drives the physical banner's base color and pattern layers.
 */
@Serializable
data class BannerFrame(
    override val lines: List<String>,
    val design: BannerDesign,
) : LayoutFrame()
