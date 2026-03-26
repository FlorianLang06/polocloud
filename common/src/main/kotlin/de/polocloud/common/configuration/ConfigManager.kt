package de.polocloud.common.configuration

import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * Central registry for all configs.
 * Call [ConfigManager.load] once at startup — that's it.
 */
object ConfigManager {

    private val configs = mutableMapOf<KClass<*>, ConfigHolder<*>>()

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Loads and registers a config. Starts a file watcher for hot reload.
     *
     * Usage:
     * ```kotlin
     * val serverConfig = ConfigManager.load(ServerConfig::class)
     * ```
     */
    fun <T : Any> load(clazz: KClass<T>): ConfigHolder<T> {
        val annotation = clazz.annotations
            .filterIsInstance<ConfigFile>()
            .firstOrNull()
            ?: error("${clazz.simpleName} is missing @ConfigFile annotation")

        @Suppress("UNCHECKED_CAST")
        return configs.getOrPut(clazz) {
            ConfigHolder(clazz, annotation.path, json).also { it.init() }
        } as ConfigHolder<T>
    }

    /**
     * Inline convenience wrapper:
     * ```kotlin
     * val serverConfig = ConfigManager.load<ServerConfig>()
     * ```
     */
    inline fun <reified T : Any> load(): ConfigHolder<T> = load(T::class)

    /**
     * Stops all file watchers. Call on application shutdown.
     */
    fun shutdown() {
        configs.values.forEach { it.stopWatcher() }
        configs.clear()
    }
}