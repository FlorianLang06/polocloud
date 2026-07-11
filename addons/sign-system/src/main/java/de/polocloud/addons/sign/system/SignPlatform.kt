package de.polocloud.addons.sign.system

import java.nio.file.Path

/**
 * A platform this addon can run on (Bukkit today; Fabric/Sponge/Nukkit can be added
 * the same way — see [de.polocloud.bridge.BridgeBootstrap] for the same shape on the
 * proxy side). Owns no sign logic itself: it only wires up [SignEntryRenderer]s per
 * [SignEntryType] and exposes what the platform-agnostic [SignSystem] needs to run.
 */
abstract class SignPlatform {

    private val renderers = mutableMapOf<SignEntryType, SignEntryRenderer>()

    /** Where [SignSystem] persists attached entries across restarts. */
    abstract val dataDirectory: Path

    /** Registers [renderer], making [renderer.type] displayable on this platform. */
    protected fun register(renderer: SignEntryRenderer) {
        renderers[renderer.type] = renderer
    }

    /** The renderer for [type], or `null` if this platform doesn't support it. */
    fun renderer(type: SignEntryType): SignEntryRenderer? = renderers[type]

    /** Every renderer registered on this platform. */
    fun renderers(): Collection<SignEntryRenderer> = renderers.values

    /** Every [SignEntryType] this platform can currently display. */
    fun supportedTypes(): Set<SignEntryType> = renderers.keys

    /** Runs [task] repeatedly, roughly every [intervalTicks] (20 ticks ≈ 1s). Drives the sign animations. */
    abstract fun scheduleRepeating(intervalTicks: Long, task: () -> Unit)
}