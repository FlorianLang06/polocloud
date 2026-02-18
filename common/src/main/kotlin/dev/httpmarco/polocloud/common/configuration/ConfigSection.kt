package dev.httpmarco.polocloud.common.configuration

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class ConfigSection(private val path: Path) {

    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type" // für polymorphe sealed classes
    }
    fun <T> readOrCreate(serializer: KSerializer<T>, default: T): T {
        return if (path.exists()) {
            val content = Files.readString(path)
            json.decodeFromString(serializer, content)
        } else {
            path.toFile().createNewFile()
            val content = json.encodeToString(serializer, default)
            Files.writeString(path, content)
            default
        }
    }

    fun <T> save(serializer: KSerializer<T>, obj: T) {
        val content = json.encodeToString(serializer, obj)
        Files.writeString(path, content)
    }
}
