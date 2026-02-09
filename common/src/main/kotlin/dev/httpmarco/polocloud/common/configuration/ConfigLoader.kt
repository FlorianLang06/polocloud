package dev.httpmarco.polocloud.config

import dev.httpmarco.polocloud.common.configuration.Config
import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.configuration.ConfigSource

/**
 * Builds a Config from multiple sources.
 * Later sources override earlier ones.
 */
class ConfigLoader {
    private val sources = mutableListOf<ConfigSource>()

    fun addSource(source: ConfigSource): ConfigLoader = apply { sources += source }

    fun load(): Config {
        val merged = mutableMapOf<String, Any?>()
        sources.forEach { merge(merged, it.load()) }
        return Config(ConfigSection(merged))
    }

    private fun merge(target: MutableMap<String, Any?>, source: Map<String, Any?>) {
        source.forEach { (key, value) ->
            val existing = target[key]
            if (value is Map<*, *> && existing is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                merge(existing as MutableMap<String, Any?>, value as Map<String, Any?>)
            } else target[key] = value
        }
    }
}
