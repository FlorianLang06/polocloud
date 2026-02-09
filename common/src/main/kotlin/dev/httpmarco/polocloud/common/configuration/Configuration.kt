package dev.httpmarco.polocloud.common.configuration

/**
 * Immutable configuration root.
 *
 * Acts as the main entry point for accessing configuration values.
 */
class Configuration internal constructor(
    private val root: ConfigSection
) {

    /**
     * Returns the root configuration section.
     */
    fun root(): ConfigSection = root

    /**
     * Returns a configuration section by dot-separated path.
     *
     * Example: "database.credentials"
     *
     * @param path configuration path
     * @return configuration section
     */
    fun section(path: String): ConfigSection =
        root.section(path)
}
