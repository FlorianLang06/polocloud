package de.polocloud.addons.sign.system.layout

/**
 * The looping sequence of [LayoutFrame]s shown for one
 * [de.polocloud.shared.service.ServiceState]. A single-frame animation is simply static.
 *
 * @param tickInterval how many animation ticks (see [de.polocloud.addons.sign.system.SignPlatform.scheduleRepeating])
 * a frame stays visible before advancing to the next one.
 */
class StateAnimation(private val frames: List<LayoutFrame>, private val tickInterval: Long = 10L) {

    fun frameAt(tick: Long): LayoutFrame {
        if (frames.isEmpty()) return LayoutFrame(emptyList(), null)
        if (frames.size == 1) return frames[0]

        val index = ((tick / tickInterval) % frames.size).toInt()
        return frames[index]
    }
}