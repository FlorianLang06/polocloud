package dev.httpmarco.polocloud.database

data class DatabaseKey<T>(val id : String, val clazz: Class<T>)