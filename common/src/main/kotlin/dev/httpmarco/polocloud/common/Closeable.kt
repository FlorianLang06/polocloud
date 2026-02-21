package dev.httpmarco.polocloud.common

interface Closeable {

    fun close(mode: ShutdownMode)

}