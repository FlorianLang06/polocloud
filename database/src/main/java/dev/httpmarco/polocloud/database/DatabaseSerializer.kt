package dev.httpmarco.polocloud.database

import com.google.gson.Gson

object DatabaseSerializer {

    private val gson = Gson()

    fun <T> serialize(value: T): String =
        gson.toJson(value)

    fun <T> deserialize(json: String, clazz: Class<T>): T =
        gson.fromJson(json, clazz)
}