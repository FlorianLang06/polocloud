package dev.httpmarco.polocloud.config

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.httpmarco.polocloud.common.configuration.ConfigException
import dev.httpmarco.polocloud.common.configuration.ConfigSource
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads configuration from a JSON file using Gson.
 *
 * The JSON structure is converted into a hierarchical
 * Map<String, Any?> representation.
 */
class FileConfigSource(
    private val file: Path,
    private val gson: Gson = Gson()
) : ConfigSource {

    override fun load(): Map<String, Any?> {
        if (!Files.exists(file)) {
            throw ConfigException("Config file does not exist: $file")
        }

        Files.newBufferedReader(file).use { reader ->
            val json = gson.fromJson(reader, JsonObject::class.java)
            return jsonObjectToMap(json)
        }
    }

    /**
     * Converts a JsonObject into a Map recursively.
     */
    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> =
        obj.entrySet().associate { (key, value) ->
            key to jsonElementToValue(value)
        }

    /**
     * Converts a JsonElement into a Kotlin value.
     */
    private fun jsonElementToValue(element: JsonElement): Any? =
        when {
            element.isJsonNull -> null

            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isBoolean -> primitive.asBoolean
                    primitive.isNumber -> {
                        val number = primitive.asNumber
                        // Preserve integer types when possible
                        if (number.toDouble() % 1 == 0.0) number.toLong()
                        else number.toDouble()
                    }
                    primitive.isString -> primitive.asString
                    else -> primitive.asString
                }
            }

            element.isJsonObject ->
                jsonObjectToMap(element.asJsonObject)

            element.isJsonArray ->
                element.asJsonArray.map { jsonElementToValue(it) }

            else -> null
        }
}
