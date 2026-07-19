package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.shared.service.ServiceState
import java.util.EnumMap

/**
 * A named, per-[ServiceState] set of [StateAnimation]s for a frame-based [SignEntryType] —
 * [SignEntryType.SIGN] and [SignEntryType.BANNER] today, each frame typed to its own kind via
 * [LayoutFrame]'s sealed hierarchy ([SignFrame]/[BannerFrame]).
 *
 * States without an explicit animation fall back to [ServiceState.UNKNOWN], so a
 * layout only needs to define the states it actually cares about.
 */
class SignLayout(id: String, type: SignEntryType = SignEntryType.SIGN) : Layout(id, type) {

    private val animations = EnumMap<ServiceState, StateAnimation>(ServiceState::class.java)

    fun set(state: ServiceState, animation: StateAnimation): SignLayout = apply {
        animations[state] = animation
    }

    fun animation(state: ServiceState): StateAnimation =
        animations[state] ?: animations[ServiceState.UNKNOWN] ?: EMPTY

    /**
     * Only the explicitly configured animations, without the [ServiceState.UNKNOWN]
     * fallback [animation] applies — used by [LayoutStorage] to persist exactly what's
     * set instead of baking the fallback into every state.
     */
    fun explicitAnimations(): Map<ServiceState, StateAnimation> = animations

    private companion object {
        val EMPTY = StateAnimation(emptyList())
    }
}