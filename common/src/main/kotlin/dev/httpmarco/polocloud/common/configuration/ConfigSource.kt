package dev.httpmarco.polocloud.common.configuration

/**
 * Represents a source of configuration values.
 */
fun interface ConfigSource {
    fun load(): Map<String, Any?>
}
