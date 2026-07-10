package de.polocloud.node.group

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Encodes a property map to a JSON string and back.
 *
 * Persisted entities ([Group]) store their properties as a single JSON `String`
 * column: the reflection-based SQL layer maps each entity field to one column and
 * has no support for `Map` fields, so a plain map would break persistence. Keeping
 * it as JSON sidesteps that while staying human-readable in the database.
 */
object PropertyCodec {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    fun encode(map: Map<String, String>): String = json.encodeToString(serializer, map)

    fun decode(raw: String): MutableMap<String, String> =
        if (raw.isBlank()) mutableMapOf()
        else runCatching { json.decodeFromString(serializer, raw).toMutableMap() }
            .getOrDefault(mutableMapOf())
}
