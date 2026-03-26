package de.polocloud.common.configuration.section

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class ConfigurationSection(private val path: Path) {

    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun <T> read(serializer: KSerializer<T>): T? {
        return if (path.exists()) {
            val content = Files.readString(path)
            json.decodeFromString(serializer, content)
        } else {
            null
        }
    }

    //TODO remove this class
}