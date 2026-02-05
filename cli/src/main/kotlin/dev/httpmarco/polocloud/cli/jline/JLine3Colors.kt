package dev.httpmarco.polocloud.cli.jline

import org.jline.jansi.Ansi

/**
 * Represents legacy Minecraft-style color codes (e.g. &a, &c, &r)
 * and translates them into ANSI escape sequences using JLine 3.
 *
 * This enum is designed for CLI applications and supports both
 * normal and bright ANSI foreground colors.
 *
 * Example usage:
 * ```
 * val message = JLine3Colors.translate("&aSuccess &7- &cError")
 * println(message)
 * ```
 */
enum class JLine3Colors(
    /**
     * The legacy color code (e.g. "&a", "&c", "&r").
     */
    val code: String,

    /**
     * The corresponding ANSI color.
     * `null` indicates a reset instruction.
     */
    color: Ansi.Color?,

    /**
     * Whether the ANSI color should be rendered in bright mode.
     */
    bright: Boolean = false
) {

    BLACK("&0", Ansi.Color.BLACK),
    DARK_BLUE("&1", Ansi.Color.BLUE),
    DARK_GREEN("&2", Ansi.Color.GREEN),
    DARK_AQUA("&3", Ansi.Color.CYAN),
    DARK_RED("&4", Ansi.Color.RED),
    DARK_PURPLE("&5", Ansi.Color.MAGENTA),
    GOLD("&6", Ansi.Color.YELLOW),
    GRAY("&7", Ansi.Color.WHITE),
    DARK_GRAY("&8", Ansi.Color.BLACK, true),
    BLUE("&9", Ansi.Color.BLUE, true),
    GREEN("&a", Ansi.Color.GREEN, true),
    AQUA("&b", Ansi.Color.CYAN, true),
    RED("&c", Ansi.Color.RED, true),
    LIGHT_PURPLE("&d", Ansi.Color.MAGENTA, true),
    YELLOW("&e", Ansi.Color.YELLOW, true),
    WHITE("&f", Ansi.Color.WHITE, true),
    RESET("&r", null);

    /**
     * Precomputed ANSI escape sequence for this color.
     */
    val ansi: String = when {
        color == null -> Ansi.ansi().reset().toString()
        bright -> Ansi.ansi().fgBright(color).toString()
        else -> Ansi.ansi().fg(color).toString()
    }

    companion object {

        /**
         * Regular expression used to detect legacy color codes.
         */
        private val COLOR_REGEX = Regex("&[0-9a-fr]")

        /**
         * Fast lookup map for color codes.
         */
        private val CODE_MAP = entries.associateBy { it.code }

        /**
         * Translates legacy color codes (e.g. "&a", "&c") in the given message
         * into ANSI escape sequences.
         *
         * By default, the message is terminated with an ANSI reset sequence
         * to prevent color bleeding in the terminal.
         *
         * @param message The input string containing legacy color codes
         * @param reset Whether to append an ANSI reset sequence at the end
         * @return The translated string with ANSI escape sequences
         */
        @JvmStatic
        fun translate(message: String, reset: Boolean = true): String {
            val result = COLOR_REGEX.replace(message) { match ->
                CODE_MAP[match.value]?.ansi ?: match.value
            }

            return if (reset) result + RESET.ansi else result
        }
    }
}
