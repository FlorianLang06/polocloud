package de.polocloud.common.version

import java.util.*

/**
 * Loads the current [PolocloudVersion] from the `version.properties` file
 * that Gradle injects into the classpath at build time.
 *
 * The resource is expected at `/version.properties` and must contain:
 * ```
 * major=3
 * minor=1
 * patch=0
 * channel=BETA
 * build=42
 * ```
 *
 * See `gradle/version.gradle.kts` for the injection logic.
 */
internal object PolocloudVersionLoader {

    private const val RESOURCE_PATH = "version.properties"

    fun load(): PolocloudVersion {
        val props = Properties()

        val stream = PolocloudVersionLoader::class.java
            .getResourceAsStream("/$RESOURCE_PATH")
            ?: throw IllegalStateException("Version resource '$RESOURCE_PATH' was not found on the classpath. Ensure version.properties is generated at build time.")

        stream.use { props.load(it) }

        return PolocloudVersion(
            major = props.requireInt("major"),
            minor = props.requireInt("minor"),
            patch = props.requireInt("patch"),
            channel = PolocloudReleaseChannel.fromString(props.require("channel")),
            build = props.require("build"),
            buildTime = props.getProperty("buildTime")?.toLongOrNull() ?: -1L,
            commitId = props.require("commitId"),
            commitIdAbbrev = props.require("commitIdAbbrev"),
        )
    }

    private fun Properties.require(key: String): String {
        return getProperty(key)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Required property '$key' is missing or blank in $RESOURCE_PATH.")
    }

    private fun Properties.requireInt(key: String): Int {
        return require(key).toIntOrNull()
            ?: throw IllegalStateException("Property '$key' must be an integer")
    }
}