package de.polocloud.common.configuration

import de.polocloud.common.configuration.ConfigurationManager.load
import de.polocloud.common.configuration.ConfigurationManager.loadResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.reflect.KClass

/**
 * Central registry for all configs.
 *
 * Provides two APIs:
 * - [load]        → simple usage, reports + throws if necessary
 * - [loadResult]  → full control via [PoloResult]
 *
 * Call once at startup to initialize configs.
 */
object ConfigurationManager {

    private val configs = mutableMapOf<KClass<*>, ConfigurationHolder<*>>()

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Loads and registers a config in a result-based manner.
     *
     * Does NOT throw — returns a [PoloResult] instead.
     *
     * Possible failures:
     * - Missing [ConfigurationFile] annotation
     *
     * Usage:
     * ```
     * val config = ConfigManager.loadResult<MyConfig>()
     *     .getOrReport() ?: return
     * ```
     */
    fun <T : Any> loadResult(clazz: KClass<T>): ConfigurationHolder<T> {
        val annotation = clazz.annotations
            .filterIsInstance<ConfigurationFile>()
            .firstOrNull()
            ?: throw IllegalStateException(
                "Missing @ConfigurationFile annotation on ${clazz.simpleName}"
            )

        @Suppress("UNCHECKED_CAST")
        return configs.getOrPut(clazz) {
            ConfigurationHolder(clazz, annotation.path, json).also { it.init() }
        } as ConfigurationHolder<T>
    }

    /**
     * Loads and registers a config.
     *
     * This is a convenience wrapper around [loadResult] that:
     * - reports errors via [ErrorReporter]
     * - throws only if the error is fatal
     *
     * Use this for simple startup scenarios.
     *
     * Usage:
     * ```
     * val config = ConfigManager.load<MyConfig>()
     * ```
     */
    fun <T : Any> load(clazz: KClass<T>): ConfigurationHolder<T> {
        return loadResult(clazz)
    }

    /**
     * Reads the config of a given path and serializer.
     * @param path
     * @param serializer
     */
    @Deprecated("This method is no longer supported!")
    fun <T> read(path: Path, serializer: KSerializer<T>): T? {
        return if (path.exists()) {
            val content = Files.readString(path)
            json.decodeFromString(serializer, content)
        } else {
            null
        }
    }

    /**
     * Inline convenience wrapper:
     * ```kotlin
     * val serverConfig = ConfigManager.load<ServerConfig>()
     * ```
     */
    inline fun <reified T : Any> load(): ConfigurationHolder<T> = load(T::class)

    /**
     * Inline variant of [loadResult].
     */
    inline fun <reified T : Any> loadResult(): ConfigurationHolder<T> = loadResult(T::class)

    /**
     * Stops all file watchers. Call on application shutdown.
     */
    fun shutdown() {
        configs.values.forEach { it.stopWatcher() }
        configs.clear()
    }
}