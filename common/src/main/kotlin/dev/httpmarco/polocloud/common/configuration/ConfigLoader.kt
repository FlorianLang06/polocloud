package dev.httpmarco.polocloud.common.configuration

import kotlin.collections.iterator

/**
 * Builds a [Config] instance from multiple configuration sources.
 *
 * Later sources override earlier ones.
 */
class ConfigLoader {

    private val sources = mutableListOf<ConfigSource>()

    /**
     * Adds a configuration source.
     */
    fun addSource(source: ConfigSource): ConfigLoader = apply {
        sources += source
    }

    /**
     * Loads and merges all configuration sources.
     */
    fun load(): Configuration {
        val merged = mutableMapOf<String, Any?>()

        sources.forEach { source ->
            merge(merged, source.load())
        }

        return Configuration(ConfigSection(merged))
    }

    private fun merge(
        target: MutableMap<String, Any?>,
        source: Map<String, Any?>
    ) {
        for ((key, value) in source) {
            val existing = target[key]
            if (value is Map<*, *> && existing is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                merge(
                    existing as MutableMap<String, Any?>,
                    value as Map<String, Any?>
                )
            } else {
                target[key] = value
            }
        }
    }
}
