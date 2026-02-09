package dev.httpmarco.polocloud.common.configuration

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.collections.get
import kotlin.reflect.KClass

/**
 * Represents a hierarchical section of configuration values.
 */
class ConfigSection internal constructor(
    val values: Map<String, Any?>
) {

    /** Access a nested section */
    fun section(path: String): ConfigSection {
        val value = resolve(path)
        if (value !is Map<*, *>) throw ConfigException("Section not found: $path")
        @Suppress("UNCHECKED_CAST")
        return ConfigSection(value as Map<String, Any?>)
    }

    /** Typed getters */
    fun getString(path: String): String = get(path, String::class)
    fun getInt(path: String): Int = get(path, Int::class)
    fun getLong(path: String): Long = get(path, Long::class)
    fun getBoolean(path: String): Boolean = get(path, Boolean::class)
    fun <T : Any> getOrDefault(path: String, default: T): T =
        try { get(path, default::class) } catch (_: Exception) { default }

    /** Generic typed getter */
    fun <T : Any> get(path: String, type: KClass<T>): T {
        val value = resolve(path)
        if (!type.isInstance(value)) {
            throw ConfigException("Invalid type for '$path', expected ${type.simpleName}")
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    /** Resolve value by dot-separated path */
    private fun resolve(path: String): Any? {
        val parts = path.split(".")
        var current: Any? = values
        for (part in parts) {
            if (current !is Map<*, *>) throw ConfigException("Invalid path: $path")
            current = current[part]
        }
        return current ?: throw ConfigException("Config value not found: $path")
    }

    /** Map this section to a Kotlin data class using Gson */
    inline fun <reified T : Any> asObject(gson: Gson = Gson()): T {
        val json = gson.toJson(values)
        return gson.fromJson(json, object : TypeToken<T>() {}.type)
    }
}
