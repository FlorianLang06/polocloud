package dev.httpmarco.polocloud.common.version

/**
 * Compares two [PolocloudVersion] instances using full semantic ordering:
 *
 * 1. Major version
 * 2. Minor version
 * 3. Patch version
 * 4. Channel stability ([PolocloudReleaseChannel.priority] — higher = more stable)
 * 5. Build number (numeric if possible, lexicographic fallback)
 */
internal object PolocloudVersionComparator {

    fun compare(a: PolocloudVersion, b: PolocloudVersion): Int {
        a.major.compareTo(b.major).takeIf { it != 0 }?.let { return it }
        a.minor.compareTo(b.minor).takeIf { it != 0 }?.let { return it }
        a.patch.compareTo(b.patch).takeIf { it != 0 }?.let { return it }
        a.channel.priority.compareTo(b.channel.priority).takeIf { it != 0 }?.let { return it }

        val aBuild = a.build.toIntOrNull()
        val bBuild = b.build.toIntOrNull()

        return when {
            aBuild != null && bBuild != null -> aBuild.compareTo(bBuild)
            else -> a.build.compareTo(b.build)
        }
    }
}