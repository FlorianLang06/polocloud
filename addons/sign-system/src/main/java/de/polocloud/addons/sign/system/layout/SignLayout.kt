package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.shared.service.ServiceState
import java.util.EnumMap

/**
 * A named, per-[ServiceState] set of [StateAnimation]s for a text-based [SignEntryType]
 * (a wooden sign today; any future type whose content is lines of text).
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

    private companion object {
        val EMPTY = StateAnimation(emptyList())
    }
}