package de.polocloud.node.services.factory.template

import de.polocloud.node.services.factory.platform.JavaVersionRange

/**
 * Resolves the minimum required Java version for a given platform version string
 * based on a sorted list of [JavaVersionRange] breakpoints.
 *
 * The algorithm sorts all ranges by their [JavaVersionRange.from] version and
 * returns the [JavaVersionRange.java] value of the last entry whose [JavaVersionRange.from]
 * is less than or equal to the queried version.
 *
 * Example with paper ranges:
 * ```
 * resolveJavaVersion("1.20.6", ranges) // → 17
 * resolveJavaVersion("1.20.5", ranges) // → 21
 * resolveJavaVersion("26.1.2", ranges) // → 21
 * resolveJavaVersion("1.16.5", ranges) // → 8
 * ```
 *
 * @param version The platform version string to evaluate (e.g. "1.20.5", "26.1.2").
 * @param ranges  Breakpoint list from [PlatformTemplate.javaVersionRanges].
 * @return Minimum required Java major version, or null if no breakpoint matches.
 */
fun resolveJavaVersion(version: String, ranges: List<JavaVersionRange>): Int? {
    val v = parseVersionSegments(version)
    return ranges
        .sortedWith { a, b -> compareSegments(parseVersionSegments(a.from), parseVersionSegments(b.from)) }
        .filter { compareSegments(v, parseVersionSegments(it.from)) >= 0 }
        .lastOrNull()
        ?.java
}
