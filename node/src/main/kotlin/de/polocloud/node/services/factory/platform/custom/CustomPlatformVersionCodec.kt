package de.polocloud.node.services.factory.platform.custom

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Encodes a [CustomPlatform]'s version list to a JSON string and back.
 *
 * Persisted the same way as [de.polocloud.node.group.TemplateCodec]: the reflection-based SQL
 * layer maps each entity field to one column and has no support for `List` fields directly.
 */
object CustomPlatformVersionCodec {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CustomPlatformVersion.serializer())

    fun encode(versions: List<CustomPlatformVersion>): String = json.encodeToString(serializer, versions)

    fun decode(raw: String): List<CustomPlatformVersion> =
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
}
