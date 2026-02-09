package dev.httpmarco.polocloud.common.configuration

/**
 * Immutable root configuration.
 */
class Config internal constructor(
    private val root: ConfigSection
) {
    fun root(): ConfigSection = root
    fun section(path: String): ConfigSection = root.section(path)
}
