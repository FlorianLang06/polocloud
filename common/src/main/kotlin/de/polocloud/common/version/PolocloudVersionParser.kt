package de.polocloud.common.version

/**
 * Parses PoloCloud version strings into [PolocloudVersion] instances.
 *
 * Supported formats:
 * - `3.1.0`            → RELEASE, no build
 * - `3.1.0-beta.12`    → BETA, build "12"
 * - `3.1.0-dev.47`     → DEV, build "47"
 * - `3.1.0-snapshot.local` → SNAPSHOT, build "local"
 */
object PolocloudVersionParser {

    private val RELEASE_PATTERN = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
    private val CHANNEL_PATTERN = Regex("""^(\d+)\.(\d+)\.(\d+)-([a-zA-Z]+)\.(.+)$""")

    /**
     * Parses the given [versionString] into a [PolocloudVersion].
     * @throws IllegalArgumentException if the format is invalid.
     */
    fun parse(versionString: String): PolocloudVersion {
        val trimmed = versionString.trim()

        RELEASE_PATTERN.matchEntire(trimmed)?.let { match ->
            val (major, minor, patch) = match.destructured
            return PolocloudVersion(major.toInt(), minor.toInt(), patch.toInt(), PolocloudReleaseChannel.RELEASE)
        }

        CHANNEL_PATTERN.matchEntire(trimmed)?.let { match ->
            val (major, minor, patch, channel, build) = match.destructured
            return PolocloudVersion(
                major.toInt(),
                minor.toInt(),
                patch.toInt(),
                PolocloudReleaseChannel.fromString(channel),
                build
            )
        }

        throw IllegalArgumentException("Invalid PoloCloud version format: '$versionString'")
    }

    /**
     * Like [parse], but returns `null` instead of throwing on invalid input.
     */
    fun parseOrNull(versionString: String): PolocloudVersion? =
        runCatching { parse(versionString) }.getOrNull()
}