package de.polocloud.cli.communication.connection.lifecycle

interface ConnectionLifecycle {
    suspend fun start()
    fun stop()
}