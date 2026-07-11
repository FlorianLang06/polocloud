package de.polocloud.addons.sign.system.layout

/**
 * Factory for the dot-based border lines used by the built-in [LayoutRegistry] layouts.
 *
 * [slidingDot] is the classic "searching for a free server" spinner: one dot
 * highlighted in [accent], sliding left to right across [count] dots, one dot
 * further per returned frame. [staticDots] is the non-animated counterpart for
 * states that don't need to move (e.g. a running server).
 */
object SignAnimations {

    fun slidingDot(count: Int = 9, symbol: String = "⚫", base: String = "§8", accent: String = "§7"): List<String> =
        (0 until count).map { highlighted ->
            (0 until count).joinToString(" ") { index -> "${if (index == highlighted) accent else base}$symbol" }
        }

    fun staticDots(count: Int = 9, symbol: String = "⚫", color: String = "§8"): String =
        (0 until count).joinToString(" ") { "$color$symbol" }
}
