package de.polocloud.addons.sign.system.spigot.renderer

import de.polocloud.addons.sign.system.SignEntry
import de.polocloud.addons.sign.system.SignEntryRenderer
import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.addons.sign.system.layout.BannerFrame
import de.polocloud.addons.sign.system.layout.LayoutFrame
import de.polocloud.addons.sign.system.spigot.renderer.hologram.BukkitHologram
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.Banner
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType

/**
 * Renders [SignEntryType.BANNER] entries: [BannerFrame.design] (base color + pattern layers)
 * is applied to the physical banner block, and [BannerFrame.lines] is shown on a
 * [BukkitHologram] floating above it — banners themselves can't display text, unlike a sign.
 */
class BukkitBannerRenderer : SignEntryRenderer(SignEntryType.BANNER), BukkitBlockMatcher {

    override fun matches(material: Material): Boolean = material.name.endsWith("_BANNER")

    override fun render(entry: SignEntry, frame: LayoutFrame) {
        val bannerFrame = frame as? BannerFrame ?: return

        entry.position.blockStateAt<Banner>()?.let { banner ->
            banner.baseColor = DyeColor.valueOf(bannerFrame.design.baseColor)
            banner.patterns = bannerFrame.design.patterns.map { Pattern(DyeColor.valueOf(it.color), PatternType.valueOf(it.pattern)) }
            banner.update()
        }

        BukkitHologram.show(entry.position, bannerFrame.lines, bannerFrame.design.hologram)
    }

    override fun remove(entry: SignEntry) {
        BukkitHologram.hide(entry.position)
    }
}
