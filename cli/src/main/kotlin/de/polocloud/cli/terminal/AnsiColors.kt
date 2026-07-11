package de.polocloud.cli.terminal

/**
 * Represents legacy Minecraft-style color codes (e.g. `&a`, `&c`, `&r`)
 * and translates them into ANSI escape sequences for use in the CLI terminal.
 *
 * Supports all 16 standard Minecraft color codes as well as a reset (`&r`) instruction.
 * Colors can be rendered in normal or bright mode depending on the entry configuration.
 *
 * Escape sequences are built from raw SGR codes rather than jline/jansi's `Ansi` builder:
 * this class is on the hot path of every log line, including ones emitted while
 * dependencies (possibly jline/jansi itself) are still being downloaded on other threads
 * during early bootstrap. Enum class initialization in the JVM is all-or-nothing and
 * permanent — if touching a jline class here raced ahead of it landing on the classpath,
 * the whole enum would be irrecoverably broken (`NoClassDefFoundError`) for the rest of
 * the process, not just that one call. Depending on nothing but core Java sidesteps that.
 *
 * Example:
 * ```kotlin
 * val colored = AnsiColor.translate("&aSuccess &7- &cError")
 * println(colored)
 * ```
 */
enum class AnsiColors(
    /**
     * The legacy color code (e.g. "&a", "&c", "&r").
     */
    val code: String,

    /**
     * The base SGR foreground color number (0-7, standard ANSI order).
     * `null` indicates a reset instruction.
     */
    color: Int?,

    /**
     * Whether the ANSI color should be rendered in bright mode.
     */
    bright: Boolean = false
) {

    BLACK("&0", 0),
    DARK_BLUE("&1", 4),
    DARK_GREEN("&2", 2),
    DARK_AQUA("&3", 6),
    DARK_RED("&4", 1),
    DARK_PURPLE("&5", 5),
    GOLD("&6", 3),
    GRAY("&7", 7),
    DARK_GRAY("&8", 0, true),
    BLUE("&9", 4, true),
    GREEN("&a", 2, true),
    AQUA("&b", 6, true),
    RED("&c", 1, true),
    LIGHT_PURPLE("&d", 5, true),
    YELLOW("&e", 3, true),
    WHITE("&f", 7, true),
    RESET("&r", null);

    /**
     * Precomputed ANSI escape sequence for this color.
     */
    val ansi: String = when {
        color == null -> "[0m"
        bright -> "[9${color}m"
        else -> "[3${color}m"
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
