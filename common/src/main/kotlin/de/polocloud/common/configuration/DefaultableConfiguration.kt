package de.polocloud.common.configuration

interface DefaultableConfiguration<T> {
    fun createDefault(): T
}