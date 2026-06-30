package de.polocloud.node.services.factory.platform

import de.polocloud.node.services.factory.template.ServiceTask

/**
 * Represents a server platform with all its resolved versions.
 *
 * @param name       Unique platform identifier (e.g. "paper", "velocity").
 * @param type       Platform role: SERVER or PROXY.
 * @param language   Runtime language used to launch the platform (e.g. JAVA).
 * @param jvmArgs           JVM arguments placed before `-jar` (e.g. "-Dcom.mojang.eula.agree=true").
 * @param globalArgs        JVM flags and program arguments applied when starting a process.
 * @param tasks             Pre-start configuration tasks referenced by key and version range.
 * @param javaVersionRanges Ordered breakpoints mapping version ranges to minimum required Java versions.
 * @param versions          All available versions resolved from the remote API.
 */
data class Platform(
    val name: String,
    val type: String,
    val language: String,
    val jvmArgs: List<String> = emptyList(),
    val globalArgs: List<String> = emptyList(),
    val tasks: List<ServiceTask> = emptyList(),
    val javaVersionRanges: List<JavaVersionRange> = emptyList(),
    val versions: List<PlatformVersion> = emptyList()
)
