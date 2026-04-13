package de.polocloud.common.configuration

import de.polocloud.common.configuration.watcher.FileWatcher
import de.polocloud.i18n.api.trError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Holds the current value of a config and manages its lifecycle.
 *
 * Features:
 * - Property delegation:  `val cfg by ConfigManager.load<Config>()`
 * - Direct access:        `configHolder.value`
 * - Automatic persistence on change
 * - Hot-reload via file watcher
 * - Change listeners
 * - Mutation API (no copy() needed)
 *
 * Write behavior:
 * Assigning a new value OR using mutate {} will:
 * 1. Update the in-memory value
 * 2. Persist it to disk
 * 3. Notify all listeners
 */
class ConfigurationHolder<T : Any>(
    private val clazz: KClass<T>,
    private val filePath: String,
    private val json: Json,
    private val serializer: KSerializer<T>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var file: Path = Path.of(filePath)
    private val listeners = mutableListOf<(T) -> Unit>()
    private var watcher: FileWatcher? = null

    @Volatile
    private var _value: T? = null

    /**
     * Current config value.
     *
     * Setting this will automatically persist + notify listeners.
     */
    var value: T
        get() {
            return _value ?: synchronized(this) {
                _value ?: loadFromDisk().also { _value = it }
            }
        }
        set(newValue) {
            _value = newValue
            persist(newValue)
            listeners.forEach { it(newValue) }
        }

    /**
     * Prevents reload loop when we write the file ourselves.
     */
    @Volatile
    private var ignoreNextReload = false


    /**
     * Mutate the current config without needing copy().
     *
     * Example:
     * ```
     * configHolder.mutate {
     *     general.locale = Locale.GERMANY
     * }
     * ```
     *
     * Requires config classes to use `var` instead of `val`.
     */
    inline fun mutate(block: T.() -> Unit) {
        val current = value
        block(current)
        value = current
    }

    fun init(enableWatcher: Boolean = false) {
        if (enableWatcher) {
            watcher = FileWatcher(file) { reload() }
        }
    }

    /**
     * Reload config from disk and notify listeners.
     */
    fun reload() {
        if (ignoreNextReload) {
            ignoreNextReload = false
            return
        }

        runCatching { loadFromDisk() }
            .onSuccess { new ->
                _value = new
                listeners.forEach { it(new) }
            }
            .onFailure { exception ->
                logger.trError("common", "configuration.parse_failed", exception)
            }
    }

    /**
     * Persist config to disk.
     */
    private fun persist(value: T) {
        ignoreNextReload = true

        file.toFile().apply {
            parentFile?.mkdirs()
            writeText(json.encodeToString(serializer, value))
        }
    }

    /**
     * Register a callback that fires whenever the config changes.
     */
    fun onChange(block: (T) -> Unit): ConfigurationHolder<T> {
        listeners += block
        return this
    }

    /**
     * Delegate operator — allows:
     * `val cfg by ConfigManager.load<Config>()`
     */
    operator fun getValue(thisRef: Any?, property: Any?): T = value

    internal fun stopWatcher() = watcher?.stop()

    private fun loadFromDisk(): T {
        if (!file.toFile().exists()) {
            val default = json.decodeFromString(serializer, "{}")

            file.toFile().apply {
                parentFile?.mkdirs()
                writeText(json.encodeToString(serializer, default))
            }

            return default
        }

        return runCatching {
            json.decodeFromString(serializer, file.toFile().readText())
        }.getOrElse { exception ->
            throw IllegalStateException("Failed to parse configuration file $file", exception)
        }
    }
}