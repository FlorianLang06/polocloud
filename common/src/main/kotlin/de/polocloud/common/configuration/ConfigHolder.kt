package de.polocloud.common.configuration

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
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
class ConfigHolder<T : Any>(
    private val clazz: KClass<T>,
    private val filePath: String,
    private val json: Json,
) {
    private var file: Path = Path.of(filePath)
    private val listeners = mutableListOf<(T) -> Unit>()
    private var watcher: FileWatcher? = null

    // Resolved once at construction — gives a clear error if @Serializable is missing
    private val serializer: KSerializer<T> = resolveSerializer()

    @Volatile
    private var _value: T = loadFromDisk()

    /**
     * Current config value.
     *
     * Setting this will automatically persist + notify listeners.
     */
    var value: T
        get() = _value
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

    /**
     * Returns a new holder using a different file path.
     * Useful for tests or multiple instances.
     */
    fun atPath(path: String): ConfigHolder<T> {
        return ConfigHolder(clazz, path, json).also { it.init() }
    }

    internal fun init() {
        watcher = FileWatcher(file) {
            reload()
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
            .onFailure { it.printStackTrace() }
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
    fun onChange(block: (T) -> Unit): ConfigHolder<T> {
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
            val default = createDefaultInstance()

            file.toFile().apply {
                parentFile?.mkdirs()
                writeText(json.encodeToString(serializer, default))
            }

            return default
        }

        return json.decodeFromString(serializer, file.toFile().readText())
    }

    @Suppress("UNCHECKED_CAST")
    private fun createDefaultInstance(): T {
        if (DefaultableConfig::class.java.isAssignableFrom(clazz.java)) {
            val instance = clazz.constructors
                .firstOrNull { it.parameters.isEmpty() }
                ?.call()

            if (instance is DefaultableConfig<*>) {
                return (instance as DefaultableConfig<T>).createDefault()
            }
        }

        val staticMethod = clazz.java.methods.firstOrNull {
            it.name == "default" &&
                    it.parameterCount == 0 &&
                    java.lang.reflect.Modifier.isStatic(it.modifiers)
        }

        if (staticMethod != null) {
            return staticMethod.invoke(null) as T
        }

        val companionField = runCatching {
            clazz.java.getDeclaredField("Companion")
        }.getOrNull()

        if (companionField != null) {
            val companion = companionField.get(null)

            if (companion is DefaultableConfig<*>) {
                return (companion as DefaultableConfig<T>).createDefault()
            }
        }

        val ctor = clazz.constructors.firstOrNull { c ->
            c.parameters.all { it.isOptional }
        }

        if (ctor != null) {
            return ctor.callBy(emptyMap())
        }

        error(
            "${clazz.simpleName} must either:\n" +
                    "- implement DefaultableConfig\n" +
                    "- provide companion object default()\n" +
                    "- OR have all constructor parameters optional"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveSerializer(): KSerializer<T> {
        return serializerOrNull(clazz.java) as? KSerializer<T>
            ?: error(
                "${clazz.simpleName} is not serializable. " +
                        "Did you add @Serializable to the class?"
            )
    }
}