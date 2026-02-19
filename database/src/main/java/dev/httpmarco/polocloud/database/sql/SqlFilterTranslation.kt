package dev.httpmarco.polocloud.database.sql

data class SqlFilterTranslation(
    val clause: String,
    val parameters: List<Any?>
)
