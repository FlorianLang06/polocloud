package de.polocloud.common.version

import de.polocloud.common.error.extensions.getOrReportAndThrow

/**
 * Represents a PoloCloud version with full channel and build metadata.
 *
 * Version format:
 * - RELEASE:  `3.1.0`
 * - Others:   `3.1.0-beta.12` / `3.1.0-dev.local`
 *
 * Use [PolocloudVersion.CURRENT] to get the running instance's version.
 * Use [PolocloudVersionParser] to parse version strings from the network or config.
 *
 * @param major Breaking changes
 * @param minor New features, backwards compatible
 * @param patch Bug fixes
 * @param channel Release channel — determines debug mode and stability
 * @param build Build identifier: CI run number, git SHA, or "local"
 */
data class PolocloudVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val channel: PolocloudReleaseChannel = PolocloudReleaseChannel.RELEASE,
    val build: String = "local",
    val buildTime: Long = -1L,
    val commitId: String = "unknown",
    val commitIdAbbrev: String = "unknown",
) : Comparable<PolocloudVersion> {

    /**
     * Whether debug logging and diagnostics are active for this build.
     */
    val isDebugEnabled: Boolean get() = channel.isDebugEnabled

    /**
     * True only for [PolocloudReleaseChannel.RELEASE] builds.
     */
    val isStable: Boolean get() = channel.isStable

    /**
     * True for any non-release build.
     */
    val isPreRelease: Boolean get() = !isStable

    /**
     * Returns the canonical version string.
     * Example: `3.1.0` or `3.1.0-beta.12`
     */
    fun toVersionString(): String = when (channel) {
        PolocloudReleaseChannel.RELEASE -> "$major.$minor.$patch"
        else -> "$major.$minor.$patch-${channel.name.lowercase()}.$build"
    }

    /**
     * Returns a human-readable display string.
     * Example: `v3.1.0` or `v3.1.0 Beta (Build 12, a3d6e84)`
     */
    fun toDisplayString(): String = when (channel) {
        PolocloudReleaseChannel.RELEASE -> "v$major.$minor.$patch ($commitIdAbbrev)"
        else -> "v$major.$minor.$patch ${channel.displayName} (Build $build, $commitIdAbbrev)"
    }

    /**
     * True if this version is strictly newer than [other].
     */
    fun isNewerThan(other: PolocloudVersion): Boolean = this > other

    /**
     * True if this version is strictly older than [other].
     */
    fun isOlderThan(other: PolocloudVersion): Boolean = this < other

    /**
     * True if major, minor and patch match — ignoring channel and build.
     */
    fun isSameRelease(other: PolocloudVersion): Boolean =
        major == other.major && minor == other.minor && patch == other.patch

    override fun compareTo(other: PolocloudVersion): Int =
        PolocloudVersionComparator.compare(this, other)

    override fun toString(): String = toVersionString()

    companion object {
        /**
         * The version of the currently running PoloCloud instance. Loaded once from classpath.
         */
        val CURRENT: PolocloudVersion by lazy {
            PolocloudVersionLoader.load().getOrReportAndThrow()
        }
    }
}