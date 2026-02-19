package dev.httpmarco.polocloud.database

import kotlin.reflect.KClass

data class DatabaseKey<T : Any>(val id : String, val clazz: KClass<T>)