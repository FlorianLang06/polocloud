package de.polocloud.common.version

import de.polocloud.common.error.exception.PoloResult
import de.polocloud.common.version.error.PolocloudVersionError
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

    fun load(): PoloResult<PolocloudVersion> {
        // fallback version
//        val systemVersion = System.getProperty("version")
//        if (systemVersion != null) {
//            return PolocloudVersionParser.parseOrNull(systemVersion)
//                ?: error("Invalid version in system property: $systemVersion")
//        }

        val props = Properties()
        val stream = PolocloudVersionLoader::class.java
            .getResourceAsStream("/$RESOURCE_PATH")
            ?: return PolocloudVersionError.ResourceNotFound(RESOURCE_PATH).asFailure()

        stream.use { props.load(it) }

        val major = props.require("major") ?: return PolocloudVersionError.MissingProperty("major").asFailure()
        val minor = props.require("minor") ?: return PolocloudVersionError.MissingProperty("minor").asFailure()
        val patch = props.require("patch") ?: return PolocloudVersionError.MissingProperty("patch").asFailure()
        val channel = props.require("channel") ?: return PolocloudVersionError.MissingProperty("channel").asFailure()
        val build = props.require("build") ?: return PolocloudVersionError.MissingProperty("build").asFailure()
        val commitId = props.require("commitId") ?: return PolocloudVersionError.MissingProperty("commitId").asFailure()
        val commitIdAbbrev = props.require("commitIdAbbrev") ?: return PolocloudVersionError.MissingProperty("commitIdAbbrev").asFailure()

        return Result.success(
            PolocloudVersion(
                major = major.toInt(),
                minor = minor.toInt(),
                patch = patch.toInt(),
                channel = PolocloudReleaseChannel.fromString(channel),
                build = build,
                buildTime = props.getProperty("buildTime")?.toLongOrNull() ?: -1L,
                commitId = commitId,
                commitIdAbbrev = commitIdAbbrev,
            )
        )
    }

    private fun Properties.require(key: String): String? = getProperty(key)?.takeIf { it.isNotBlank() }
}