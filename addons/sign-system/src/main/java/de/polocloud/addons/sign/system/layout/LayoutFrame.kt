package de.polocloud.addons.sign.system.layout

/** One still image of a [StateAnimation] — the text lines shown while it's this frame's turn. */
data class LayoutFrame(
    val lines: List<String>
)