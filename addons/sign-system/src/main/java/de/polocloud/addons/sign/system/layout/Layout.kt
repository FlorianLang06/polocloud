package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType

/**
 * A named set of display content for one [SignEntryType], picked by
 * [de.polocloud.addons.sign.system.SignEntry.layoutId].
 *
 * Intentionally abstract rather than a single class: [SignLayout] models the
 * text-line content a sign renders, but a painting or banner layout would carry
 * entirely different content (an image reference, a set of patterns, ...) once
 * those renderers exist — this base only fixes the identity ([id], [type]) every
 * layout shares.
 */
abstract class Layout(val id: String, val type: SignEntryType)