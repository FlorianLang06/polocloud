package de.polocloud.addons.sign.system.layout

import kotlinx.serialization.Serializable

/**
 * The looping sequence of [LayoutFrame]s shown for one
 * [de.polocloud.shared.service.ServiceState]. A single-frame animation is simply static.
 *
 * @param tickInterval how many animation ticks (see [de.polocloud.addons.sign.system.SignPlatform.scheduleRepeating])
 * a frame stays visible before advancing to the next one.
 */
@Serializable
class StateAnimation(private val frames: List<LayoutFrame>, private val tickInterval: Long = 10L) {

    /** The frame to show at [tick], or `null` if this animation has no frames at all — [LayoutFrame] is
     *  sealed per [de.polocloud.addons.sign.system.SignEntryType], so there's no type-neutral "empty" frame to fabricate. */
    fun frameAt(tick: Long): LayoutFrame? {
        if (frames.isEmpty()) return null
        if (frames.size == 1) return frames[0]

        val index = ((tick / tickInterval) % frames.size).toInt()
        return frames[index]
    }
}