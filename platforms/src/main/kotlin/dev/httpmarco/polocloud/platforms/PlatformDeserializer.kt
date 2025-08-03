package dev.httpmarco.polocloud.platforms

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import dev.httpmarco.polocloud.platforms.bridge.Bridge
import dev.httpmarco.polocloud.v1.GroupType

class PlatformDeserializer : JsonDeserializer<Platform> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Platform {
        val obj = json.asJsonObject

        return Platform(
            name = obj["name"].asString,
            url = obj["url"].asString,
            language = context.deserialize(obj["language"], PlatformLanguage::class.java),
            shutdownCommand = obj.get("shutdownCommand")?.asString ?: "stop",
            type = context.deserialize(obj["type"], GroupType::class.java),
            arguments = obj.get("arguments")?.let {
                context.deserialize<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList(),
            flags = obj.get("flags")?.let {
                context.deserialize<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList(),
            versions = context.deserialize(obj["versions"], object : TypeToken<List<PlatformVersion>>() {}.type),
            bridge = obj.get("bridge")?.let { context.deserialize<Bridge>(it, Bridge::class.java) },
            bridgePath = obj.get("bridgePath")?.asString,
            tasks = obj.get("tasks")?.let {
                context.deserialize<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList(),
            preTasks = obj.get("preTasks")?.let {
                context.deserialize<List<String>>(it, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList(),
            copyServerIcon = obj.get("copyServerIcon")?.asBoolean ?: true,
            setFileName = obj.get("setFileName")?.asBoolean ?: true,
        )
    }
}