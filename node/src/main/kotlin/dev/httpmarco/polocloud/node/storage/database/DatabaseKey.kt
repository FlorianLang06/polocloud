package dev.httpmarco.polocloud.node.storage.database

data class DatabaseKey<T>(val id : String, val clazz: Class<T>)
