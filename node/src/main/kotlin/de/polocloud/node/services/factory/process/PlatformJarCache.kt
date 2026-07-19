package de.polocloud.node.services.factory.process

import java.io.File

/**
 * Shared cache directory holding platform JARs, reused across services and node restarts.
 *
 * [PlatformProcess] downloads URL-sourced versions into it lazily, on first use. Custom
 * local-file versions are instead written here eagerly, at attach time (see
 * `de.polocloud.node.services.factory.platform.custom.CustomPlatformService.addVersion`), so a
 * live jar never depends on the operator's original file surviving until a service starts.
 * Both sides need to agree on the exact same file for a given platform/version, hence this
 * being pulled out of [PlatformProcess] rather than duplicated.
 */
object PlatformJarCache {

    val DIRECTORY = File(".cache/platforms/versions")

    /** The cache file for `<platformName>`'s version `<version>`/`<build>`, matching [PlatformProcess]'s naming. */
    fun fileFor(platformName: String, version: String, build: Int): File =
        File(DIRECTORY, "$platformName-$version-$build.jar")
}
