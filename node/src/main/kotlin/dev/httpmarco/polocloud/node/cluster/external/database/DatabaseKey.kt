package dev.httpmarco.polocloud.node.cluster.external.database

data class DatabaseKey<T>(val id : String, val clazz: Class<T>)
