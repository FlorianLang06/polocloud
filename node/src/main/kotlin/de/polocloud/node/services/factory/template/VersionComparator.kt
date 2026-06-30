package de.polocloud.node.services.factory.template

/**
 * Shared helpers for comparing platform version strings (e.g. "1.20.5", "26.1.2").
 *
 * Both the Java-version resolution and the task version-range matching rely on the
 * same numeric, segment-wise comparison semantics, so the logic lives here once.
 */

/**
 * Parses a version string into a list of integer segments.
 * Pre-release suffixes such as `-SNAPSHOT`, `-rc1`, or `-pre2` are stripped before parsing.
 *
 * @param version Raw version string (e.g. "1.20.5", "1.21.9-pre2", "26.1").
 * @return Numeric segments, e.g. `[1, 20, 5]` or `[26, 1]`.
 */
internal fun parseVersionSegments(version: String): List<Int> =
    version.substringBefore("-").split(".").mapNotNull { it.toIntOrNull() }

/**
 * Compares two parsed version segment lists lexicographically.
 * Shorter lists are padded with zeros for comparison purposes.
 *
 * @return Negative if [a] < [b], zero if equal, positive if [a] > [b].
 */
internal fun compareSegments(a: List<Int>, b: List<Int>): Int {
    val len = maxOf(a.size, b.size)
    for (i in 0 until len) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Compares two raw version strings using [parseVersionSegments] and [compareSegments].
 *
 * @return Negative if [a] < [b], zero if equal, positive if [a] > [b].
 */
internal fun compareVersions(a: String, b: String): Int =
    compareSegments(parseVersionSegments(a), parseVersionSegments(b))
