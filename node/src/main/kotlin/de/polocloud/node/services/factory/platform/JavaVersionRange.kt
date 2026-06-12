package de.polocloud.node.services.factory.platform

import kotlinx.serialization.Serializable

/**
 * Defines a breakpoint at which a new minimum Java version becomes required.
 *
 * Ranges are evaluated in order: the last entry whose [from] is less than or
 * equal to the current platform version determines the required Java version.
 *
 * @param from Minimum platform version (inclusive) that requires [java] (e.g. "1.17", "1.20.5").
 * @param java Minimum required Java major version (e.g. 8, 16, 17, 21).
 */
@Serializable
data class JavaVersionRange(
    val from: String,
    val java: Int
)
