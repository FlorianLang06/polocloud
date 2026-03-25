package de.polocloud.common.configuration

interface DefaultableConfig<T> {
    fun createDefault(): T
}