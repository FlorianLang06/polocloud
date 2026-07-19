package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType

/**
 * A named set of display content for one [SignEntryType], picked by
 * [de.polocloud.addons.sign.system.SignEntry.layoutId].
 *
 * Intentionally abstract rather than a single class: [SignLayout] covers every
 * frame-based type today (sign and banner alike — see [LayoutFrame]'s sealed
 * hierarchy for how those two differ), but a future type whose content isn't
 * frames of lines at all (a painting's image reference, say) would need its own
 * [Layout] subtype entirely — this base only fixes the identity ([id], [type])
 * every layout shares.
 */
abstract class Layout(val id: String, val type: SignEntryType)