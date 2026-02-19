package dev.httpmarco.polocloud.database.filtering

interface FilterTranslator<Q> {
    fun translate(filter: Filter): Q
}