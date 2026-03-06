package dev.httpmarco.polocloud.common.version

import java.util.Properties

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

        val stream = PolocloudVersionLoader::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("version.properties not found in classpath. Did Gradle inject it correctly?")

        stream.use { props.load(it) }

        return PolocloudVersion(
            major = props.require("major").toInt(),
            minor = props.require("minor").toInt(),
            patch = props.require("patch").toInt(),
            channel = PolocloudReleaseChannel.fromString(props.require("channel")),
            build = props.require("build"),
            buildTime = props.getProperty("buildTime")?.toLongOrNull() ?: -1L
        )
    }

    private fun Properties.require(key: String): String =
        getProperty(key)?.takeIf { it.isNotBlank() }
            ?: error("Missing required key '$key' in version.properties")
}