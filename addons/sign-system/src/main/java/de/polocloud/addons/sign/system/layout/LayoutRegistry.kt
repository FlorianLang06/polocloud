package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.shared.service.ServiceState
import org.bukkit.Material

/**
 * Holds every registered [Layout], keyed by id + [SignEntryType].
 *
 * Ships a "default" [SignLayout] for [SignEntryType.SIGN] out of the box; platforms
 * or other addons can [register] further layouts (a painting/banner default once
 * those renderers exist, server-specific themes, ...) without touching this class.
 */
class LayoutRegistry {

    private val layouts = linkedSetOf<Layout>()

    init {
        register(defaultSignLayout())
    }

    fun register(layout: Layout) {
        layouts += layout
    }

    fun find(id: String, type: SignEntryType): SignLayout? =
        layouts.filterIsInstance<SignLayout>().firstOrNull { it.id == id && it.type == type }

    /**
     * The built-in "default" sign layout. [ServiceState.UNKNOWN]/[ServiceState.QUEUED]/
     * [ServiceState.STARTING] reuse the sliding-dot "searching" spinner — the same
     * loading sequence the sign system always shipped, now actually animated: each
     * frame slides the highlighted (§7) dot one step further through the §8 dots,
     * left to right.
     */
    private fun defaultSignLayout(): SignLayout {
        fun bordered(top: String, bottom: String, borders: List<String>) =
            borders.map { border -> LayoutFrame(listOf(border, top, bottom, border), Material.CYAN_TERRACOTTA.toString()) }

        val searching = SignAnimations.slidingDot()
        val stoppingBorder = SignAnimations.staticDots(color = "§c")
        val stoppedBorder = SignAnimations.staticDots(color = "§8")

        return SignLayout("default", SignEntryType.SIGN)
            .set(ServiceState.UNKNOWN, StateAnimation(bordered("§0Server", "§0wird gesucht", searching), tickInterval = 4L))
            .set(ServiceState.QUEUED, StateAnimation(bordered("§7%group%", "§7in Warteschlange", searching), tickInterval = 4L))
            .set(ServiceState.STARTING, StateAnimation(bordered("§7%group%", "§estartet ...", searching), tickInterval = 4L))
            .set(
                ServiceState.RUNNING,
                StateAnimation(listOf(LayoutFrame(listOf("§8► §0%service% §8◄", "§a§lBetreten", "§0%state%", "§8⚫ §0%online%/%max% §8⚫"),
                    Material.LIME_TERRACOTTA.toString())))
            )
            .set(
                ServiceState.STOPPING,
                StateAnimation(listOf(LayoutFrame(listOf(stoppingBorder, "§7%service%", "§cstoppt ...", stoppingBorder), Material.RED_TERRACOTTA.toString())))
            )
            .set(
                ServiceState.STOPPED,
                StateAnimation(listOf(LayoutFrame(listOf(stoppedBorder, "§7%group%", "§8offline", stoppedBorder),Material.RED_TERRACOTTA.toString())))
            )
    }
}
