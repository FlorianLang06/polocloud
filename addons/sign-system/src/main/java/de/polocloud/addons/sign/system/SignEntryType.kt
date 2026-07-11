package de.polocloud.addons.sign.system

/**
 * The kind of physical object a [SignEntry] is rendered onto.
 *
 * Deliberately not a closed enum: a platform registers one [SignEntryRenderer] per
 * type it supports (see [SignPlatform.register]), so a new kind — a painting, a
 * banner, whatever a future platform module wants to draw a group's state onto —
 * can be added without touching this module. [SIGN] is the only type with a shipped
 * renderer today; [PAINTING] and [BANNER] exist so addons can already target them.
 */
data class SignEntryType(val id: String) {

    override fun toString(): String = id

    companion object {
        val SIGN = SignEntryType("sign")
        val PAINTING = SignEntryType("painting")
        val BANNER = SignEntryType("banner")
    }
}