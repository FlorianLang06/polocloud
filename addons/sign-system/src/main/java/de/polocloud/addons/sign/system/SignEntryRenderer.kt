package de.polocloud.addons.sign.system

import de.polocloud.addons.sign.system.layout.LayoutFrame

/**
 * Renders one [SignEntryType] (a sign, a painting, a banner, ...) on a specific
 * platform. A platform registers one renderer per type it supports (see
 * [SignPlatform.register]); [SignSystem] never touches the platform API directly,
 * it only calls through here.
 */
abstract class SignEntryRenderer(val type: SignEntryType) {

    /** Applies [frame] (already placeholder-resolved) onto the object at [entry]'s position. */
    abstract fun render(entry: SignEntry, frame: LayoutFrame)

    /** Reverts whatever [render] placed, e.g. blanks a sign's lines. */
    abstract fun remove(entry: SignEntry)
}