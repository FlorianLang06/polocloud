package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import org.tomlj.Toml
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Serializable
@SerialName("PlatformFilePropertyUpdateAction")
class PlatformFilePropertyUpdateAction(private val key: String, private val value: String) : PlatformAction() {

    override fun run(
        file: Path,
        step: PlatformTaskStep,
        environment: Map<String, String>
    ) {
        file.parent.createDirectories()

        val translatedValue = modifyValueWithEnvironment(value, environment)

        if (step.filename.endsWith(".properties")) {
            val properties = Properties()
            if (file.exists()) {
                FileInputStream(file.toFile()).use { input -> properties.load(input) }
            }

            properties.setProperty(key, modifyValueWithEnvironment(translatedValue, environment))

            FileOutputStream(file.toFile()).use { output ->
                properties.store(
                    output,
                    "Update configuration property: $key"
                )
            }
        } else if (step.filename.endsWith(".yml")) {
            val loader = YamlConfigurationLoader.builder()
                .path(file)
                .build()

            val root = if (file.exists()) loader.load() else loader.createNode()
            val path = key.split(".")

            if (translatedValue == "true" || translatedValue == "false") {
                // Convert boolean strings to actual booleans
                root.node(*path.toTypedArray()).set(translatedValue.toBoolean())
            } else if (translatedValue.toIntOrNull() != null) {
                // Convert numeric strings to actual integers
                root.node(*path.toTypedArray()).set(translatedValue.toInt())
            } else {
                // Otherwise, treat it as a string
                root.node(*path.toTypedArray()).set(translatedValue)
            }
            loader.save(root)
        } else if (step.filename.endsWith(".toml")) {
            val pathList = key.split(".")
            val fileText = if (file.exists()) file.toFile().readText() else ""
            val toml = Toml.parse(fileText)

            val existingMap = toml.toMap().toMutableMap()

            fun setValue(map: MutableMap<String, Any?>, keys: List<String>, value: Any?) {
                if (keys.size == 1) {
                    map[keys[0]] = value
                } else {
                    val subMap = map.getOrPut(keys[0]) {
                        mutableMapOf<String, Any?>()
                    } as? MutableMap<String, Any?> ?: error("Conflict at key: ${keys[0]}")
                    setValue(subMap, keys.drop(1), value)
                }
            }
            val valueToSet: Any = when {
                translatedValue.equals("true", ignoreCase = true) -> true
                translatedValue.equals("false", ignoreCase = true) -> false
                translatedValue.toIntOrNull() != null -> translatedValue.toInt()
                else -> translatedValue
            }
            setValue(existingMap, pathList, valueToSet)
            val newTomlContent = buildToml(existingMap)
            file.toFile().writeText(newTomlContent)
        }
    }

    fun buildToml(map: Map<String, Any?>, prefix: String = ""): String {
        val builder = StringBuilder()
        for ((key, value) in map) {
            val fullKey = if (prefix.isNotEmpty()) "$prefix.$key" else key
            when (value) {
                is Map<*, *> -> builder.append(buildToml(value as Map<String, Any?>, fullKey))
                is String -> builder.append("$fullKey = \"${value.replace("\"", "\\\"")}\"\n")
                else -> builder.append("$fullKey = $value\n")
            }
        }
        return builder.toString()
    }
}