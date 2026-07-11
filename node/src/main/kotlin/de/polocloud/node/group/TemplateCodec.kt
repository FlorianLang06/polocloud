package de.polocloud.node.group

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Encodes a group's template name list to a JSON string and back.
 *
 * Persisted entities ([Group]) store their templates as a single JSON `String`
 * column, for the same reason as [PropertyCodec]: the reflection-based SQL layer
 * maps each entity field to one column and has no support for `List` fields.
 */
object TemplateCodec {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val serializer = ListSerializer(String.serializer())

    fun encode(templates: List<String>): String = json.encodeToString(serializer, templates)

    fun decode(raw: String): List<String> =
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
}
