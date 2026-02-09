package dev.httpmarco.polocloud.common.configuration

import kotlin.collections.get
import kotlin.reflect.KClass

/**
 * Represents a hierarchical section of configuration values.
 */
class ConfigSection internal constructor(
    private val values: Map<String, Any?>
) {

    /**
     * Returns a nested configuration section.
     *
     * @param path dot-separated path
     * @throws ConfigException if the section does not exist
     */
    fun section(path: String): ConfigSection {
        val value = resolve(path)
        if (value !is Map<*, *>) {
            throw ConfigException("Section not found: $path")
        }
        @Suppress("UNCHECKED_CAST")
        return ConfigSection(value as Map<String, Any?>)
    }

    /**
     * Returns a string value.
     */
    fun getString(path: String): String =
        get(path, String::class)

    /**
     * Returns an integer value.
     */
    fun getInt(path: String): Int =
        get(path, Int::class)

    /**
     * Returns a boolean value.
     */
    fun getBoolean(path: String): Boolean =
        get(path, Boolean::class)

    /**
     * Returns a value or the given default if missing.
     */
    fun <T : Any> getOrDefault(path: String, default: T): T =
        try {
            get(path, default::class)
        } catch (_: Exception) {
            default
        }

    /**
     * Returns a required value.
     *
     * @throws ConfigException if the value is missing or null
     */
    fun <T : Any> require(path: String, type: KClass<T>): T {
        val value = get(path, type)
        return value
    }

    /**
     * Returns this configuration section as a raw map.
     */
    fun ConfigSection.toMap(): Map<String, Any?> = values

    /**
     * Generic typed getter.
     *
     * @throws ConfigException if the value does not exist or has a wrong type
     */
    fun <T : Any> get(path: String, type: KClass<T>): T {
        val value = resolve(path)
        if (!type.isInstance(value)) {
            throw ConfigException(
                "Invalid type for '$path', expected ${type.simpleName}"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun resolve(path: String): Any? {
        val parts = path.split(".")
        var current: Any? = values

        for (part in parts) {
            if (current !is Map<*, *>) {
                throw ConfigException("Invalid path: $path")
            }
            current = current[part]
        }

        return current ?: throw ConfigException("Config value not found: $path")
    }
}
