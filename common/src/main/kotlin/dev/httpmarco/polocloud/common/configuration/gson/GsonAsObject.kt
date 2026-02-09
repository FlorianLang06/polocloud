package dev.httpmarco.polocloud.common.configuration.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.httpmarco.polocloud.common.configuration.ConfigSection

/**
 * Maps a configuration section to a Kotlin object using Gson.
 */
inline fun <reified T> ConfigSection.asObject(
    gson: Gson = Gson()
): T {
    val json = gson.toJson(this.toMap())
    return gson.fromJson(json, object : TypeToken<T>() {}.type)
}
