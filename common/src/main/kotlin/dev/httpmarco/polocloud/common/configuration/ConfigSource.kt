package dev.httpmarco.polocloud.common.configuration

/**
 * Represents a source of configuration values.
 */
fun interface ConfigSource {

    /**
     * Loads configuration data as a hierarchical map.
     */
    fun load(): Map<String, Any?>
}
