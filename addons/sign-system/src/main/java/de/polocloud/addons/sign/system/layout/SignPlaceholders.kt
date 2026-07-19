package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntry

/** Resolves the well-known `%...%` placeholders supported inside a [LayoutFrame]'s lines. */
object SignPlaceholders {

    fun apply(frame: LayoutFrame, entry: SignEntry): LayoutFrame {
        val lines = frame.lines.map { resolve(it, entry) }

        return when (frame) {
            is SignFrame -> frame.copy(lines = lines)
            is BannerFrame -> frame.copy(lines = lines)
        }
    }

    private fun resolve(line: String, entry: SignEntry): String {
        val service = entry.service

        return line
            .replace("%group%", entry.group)
            .replace("%service%", service?.name() ?: entry.group)
            .replace("%online%", (service?.onlinePlayers ?: 0).toString())
            .replace("%max%", (service?.maxPlayers ?: 0).toString())
            .replace("%state%", (service?.state?.toString() ?: "UNKNOWN"))
    }
}
