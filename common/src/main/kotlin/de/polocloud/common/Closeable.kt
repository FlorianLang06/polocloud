package de.polocloud.common

interface Closeable {

    fun close(mode: ShutdownMode)

}