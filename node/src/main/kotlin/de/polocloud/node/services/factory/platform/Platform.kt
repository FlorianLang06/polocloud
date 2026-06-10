package de.polocloud.node.services.factory.platform

/**
 * Represents a server platform with all its resolved versions.
 *
 * @param name       Unique platform identifier (e.g. "paper", "velocity").
 * @param type       Platform role: SERVER or PROXY.
 * @param language   Runtime language used to launch the platform (e.g. JAVA).
 * @param globalArgs        JVM flags and program arguments applied when starting a process.
 * @param javaVersionRanges Ordered breakpoints mapping version ranges to minimum required Java versions.
 * @param versions          All available versions resolved from the remote API.
 */
data class Platform(
    val name: String,
    val type: String,
    val language: String,
    val globalArgs: List<String> = emptyList(),
    val javaVersionRanges: List<JavaVersionRange> = emptyList(),
    val versions: List<PlatformVersion> = emptyList()
)
