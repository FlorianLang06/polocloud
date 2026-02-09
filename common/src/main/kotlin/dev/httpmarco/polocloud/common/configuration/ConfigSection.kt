package dev.httpmarco.polocloud.common.configuration

import com.google.gson.GsonBuilder
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

private var GSON = GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create()

class ConfigSection(private val path: Path) {

    fun withMapping(type: Type, adapter: Any): ConfigSection {
        GSON = GSON.newBuilder().registerTypeAdapter(type, adapter).create()
        return this;
    }

    fun <T> readOrCreate(default: T): T {
        if (path.exists()) {
            return GSON.fromJson(Files.readString(path), default?.javaClass) as T
        } else {
            path.toFile().createNewFile()
            Files.writeString(path, GSON.toJson(default))
            return default
        }
    }
}