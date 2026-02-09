package dev.httpmarco.polocloud.common.configuration.sources

import dev.httpmarco.polocloud.common.configuration.ConfigSource

/**
 * Loads configuration from environment variables.
 *
 * Example:
 * DATABASE_HOST=localhost → database.host
 */
class EnvironmentConfigSource : ConfigSource {

    override fun load(): Map<String, Any?> =
        System.getenv().mapKeys { (key, _) ->
            key.lowercase().replace("_", ".")
        }
}
