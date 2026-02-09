package dev.httpmarco.polocloud.common.configuration.sources

import dev.httpmarco.polocloud.common.configuration.ConfigSource

/**
 * Loads configuration from JVM system properties.
 */
class SystemPropertyConfigSource : ConfigSource {

    override fun load(): Map<String, Any?> =
        System.getProperties().entries.associate { (k, v) ->
            k.toString() to v
        }
}
