package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntry

/** Resolves the well-known `%...%` placeholders supported inside a [LayoutFrame]'s lines. */
object SignPlaceholders {

    fun apply(frame: LayoutFrame, entry: SignEntry): LayoutFrame {
        val service = entry.service

        return LayoutFrame(
            frame.lines.map { line ->
                line
                    .replace("%group%", entry.group)
                    .replace("%service%", service?.name() ?: entry.group)
                    .replace("%online%", (service?.onlinePlayers ?: 0).toString())
                    .replace("%max%", (service?.maxPlayers ?: 0).toString())
                    .replace("%state%", (service?.state?.toString() ?: "UNKNOWN"))
            },
            frame.backgroundBlock
        )
    }
}
