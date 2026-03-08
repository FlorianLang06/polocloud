package de.polocloud.common.version

/**
 * Represents a PoloCloud release channel.
 *
 * Channels define the stability and behavior of a build:
 * - [SNAPSHOT] Local development builds — debug always on
 * - [DEV]      CI-generated development builds — debug always on
 * - [ALPHA]    Early feature testing — unstable, no debug
 * - [BETA]     Feature-complete testing — no debug
 * - [RELEASE]  Stable production builds — no debug
 *
 * The [priority] field determines ordering: higher = more stable.
 */
enum class PolocloudReleaseChannel(
    val displayName: String,
    val isDebugEnabled: Boolean,
    val isStable: Boolean,
    val priority: Int,
) {
    SNAPSHOT(
        displayName = "Snapshot",
        isDebugEnabled = true,
        isStable = false,
        priority = 0
    ),
    DEV(
        displayName = "Dev",
        isDebugEnabled = true,
        isStable = false,
        priority = 1
    ),
    ALPHA(
        displayName = "Alpha",
        isDebugEnabled = false,
        isStable = false,
        priority = 2
    ),
    BETA(
        displayName = "Beta",
        isDebugEnabled = false,
        isStable = false,
        priority = 3
    ),
    RELEASE(
        displayName = "Release",
        isDebugEnabled = false,
        isStable = true,
        priority = 4
    );

    companion object {
        fun fromString(value: String): PolocloudReleaseChannel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SNAPSHOT
    }
}