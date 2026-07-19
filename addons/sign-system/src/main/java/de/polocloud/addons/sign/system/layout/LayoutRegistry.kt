package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.shared.service.ServiceState
import org.bukkit.Material

/**
 * Holds every registered [Layout], keyed by id + [SignEntryType].
 *
 * Loaded from [storage] on construction — [defaultSignLayout]/[defaultBannerLayout] are only
 * used to seed [storage] the first time it runs (see [LayoutStorage.load]); from then on,
 * everything shown by [de.polocloud.addons.sign.system.SignSystem] comes from the operator-
 * editable file, hot-reloaded via [LayoutStorage.watch].
 */
class LayoutRegistry(private val storage: LayoutStorage) {

    private val layouts = linkedSetOf<Layout>()

    init {
        reload()
        storage.watch { reload() }
    }

    /** Re-reads every layout from [storage], replacing whatever was previously loaded/registered. */
    fun reload() {
        val loaded = storage.load { listOf(defaultSignLayout(), defaultBannerLayout()) }
        layouts.clear()
        layouts += loaded
    }

    /** Registers [layout] in memory only — does not persist it. Callers that add a layout
     *  at runtime should follow up with `storage.save(...)` if it should survive a reload. */
    fun register(layout: Layout) {
        layouts += layout
    }

    fun find(id: String, type: SignEntryType): SignLayout? =
        layouts.filterIsInstance<SignLayout>().firstOrNull { it.id == id && it.type == type }

    /** Stops watching [storage]'s file for changes — called from [de.polocloud.addons.sign.system.SignSystem.stop]. */
    fun stop() = storage.stopWatching()

    /**
     * The built-in "default" sign layout. [ServiceState.UNKNOWN]/[ServiceState.QUEUED]/
     * [ServiceState.STARTING] reuse the sliding-dot "searching" spinner — the same
     * loading sequence the sign system always shipped, now actually animated: each
     * frame slides the highlighted (§7) dot one step further through the §8 dots,
     * left to right.
     */
    private fun defaultSignLayout(): SignLayout {
        fun bordered(text: List<String>, borders: List<String>) =
            borders.map { border -> SignFrame(listOf(border, text[0], text[1], border), backgroundBlock = Material.CYAN_TERRACOTTA.toString()) }

        val searching = SignAnimations.slidingDot()
        val stoppingBorder = SignAnimations.staticDots(color = "§c")
        val stoppedBorder = SignAnimations.staticDots(color = "§8")

        return SignLayout("default", SignEntryType.SIGN)
            .set(ServiceState.UNKNOWN, StateAnimation(bordered(DefaultStatusText.SEARCHING, searching), tickInterval = 4L))
            .set(ServiceState.QUEUED, StateAnimation(bordered(DefaultStatusText.QUEUED, searching), tickInterval = 4L))
            .set(ServiceState.STARTING, StateAnimation(bordered(DefaultStatusText.STARTING, searching), tickInterval = 4L))
            .set(
                ServiceState.RUNNING,
                StateAnimation(listOf(SignFrame(listOf("§8► §0%service% §8◄", "§a§lBetreten", "§0%state%", "§8⚫ §0%online%/%max% §8⚫"),
                    backgroundBlock = Material.LIME_TERRACOTTA.toString())))
            )
            .set(
                ServiceState.STOPPING,
                StateAnimation(listOf(SignFrame(listOf(stoppingBorder) + DefaultStatusText.STOPPING + stoppingBorder, backgroundBlock = Material.RED_TERRACOTTA.toString())))
            )
            .set(
                ServiceState.STOPPED,
                StateAnimation(listOf(SignFrame(listOf(stoppedBorder) + DefaultStatusText.STOPPED + stoppedBorder, backgroundBlock = Material.RED_TERRACOTTA.toString())))
            )
    }

    /**
     * The built-in "default" banner layout — the same status wording as [defaultSignLayout]
     * (see [DefaultStatusText]), shown on the hologram above the banner instead of bordered
     * onto a sign's face, with [BannerFrame.design] cycling the physical banner's pattern per
     * state (see [de.polocloud.addons.sign.system.spigot.renderer.BukkitBannerRenderer]).
     */
    private fun defaultBannerLayout(): SignLayout {
        fun design(base: String, vararg layers: Pair<String, String>) =
            BannerDesign(base, layers.map { PatternLayer(it.first, it.second) })

        return SignLayout("default", SignEntryType.BANNER)
            .set(ServiceState.UNKNOWN, StateAnimation(listOf(BannerFrame(DefaultStatusText.SEARCHING, design("BLACK", "GRAY" to "BORDER")))))
            .set(ServiceState.QUEUED, StateAnimation(listOf(BannerFrame(DefaultStatusText.QUEUED, design("GRAY", "LIGHT_GRAY" to "BORDER")))))
            .set(ServiceState.STARTING, StateAnimation(listOf(BannerFrame(DefaultStatusText.STARTING, design("YELLOW", "ORANGE" to "BORDER")))))
            .set(
                ServiceState.RUNNING,
                StateAnimation(listOf(BannerFrame(listOf("§a§lBetreten", "§0%service%", "§8⚫ §0%online%/%max% §8⚫"), design("LIME", "GREEN" to "BORDER"))))
            )
            .set(ServiceState.STOPPING, StateAnimation(listOf(BannerFrame(DefaultStatusText.STOPPING, design("ORANGE", "RED" to "BORDER")))))
            .set(ServiceState.STOPPED, StateAnimation(listOf(BannerFrame(DefaultStatusText.STOPPED, design("RED", "BLACK" to "BORDER")))))
    }

    /**
     * Status wording shared by [defaultSignLayout] and [defaultBannerLayout] so the two media
     * agree on what a state says — a sign borders these two lines top/bottom, a banner's
     * hologram shows them as-is. [ServiceState.RUNNING] isn't here: its content genuinely
     * differs in shape per medium (a sign's bordered box has room for a name-tag + state
     * line a floating hologram doesn't need), so each layout spells it out itself.
     */
    private object DefaultStatusText {
        val SEARCHING = listOf("§0Server", "§0wird gesucht")
        val QUEUED = listOf("§7%group%", "§7in Warteschlange")
        val STARTING = listOf("§7%group%", "§estartet ...")
        val STOPPING = listOf("§7%service%", "§cstoppt ...")
        val STOPPED = listOf("§7%group%", "§8offline")
    }
}
