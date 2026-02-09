package dev.httpmarco.polocloud.common.configuration

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads configuration from a JSON file using Gson.
 */
class FileConfigSource(
    private val file: Path,
    private val gson: Gson = Gson()
) : ConfigSource {

    override fun load(): Map<String, Any?> {
        if (!Files.exists(file)) throw ConfigException("Config file does not exist: $file")
        Files.newBufferedReader(file).use { reader ->
            val json = gson.fromJson(reader, JsonObject::class.java)
            return jsonObjectToMap(json)
        }
    }

    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> =
        obj.entrySet().associate { (key, value) -> key to jsonElementToValue(value) }

    private fun jsonElementToValue(element: JsonElement): Any? =
        when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> {
                val prim = element.asJsonPrimitive
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isNumber -> {
                        val num = prim.asNumber
                        if (num.toDouble() % 1 == 0.0) num.toLong() else num.toDouble()
                    }
                    prim.isString -> prim.asString
                    else -> prim.asString
                }
            }
            element.isJsonObject -> jsonObjectToMap(element.asJsonObject)
            element.isJsonArray -> element.asJsonArray.map { jsonElementToValue(it) }
            else -> null
        }
}
