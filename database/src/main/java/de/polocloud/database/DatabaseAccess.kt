package de.polocloud.database

import de.polocloud.common.ShutdownMode

object DatabaseAccess {

    private lateinit var connection : DatabaseConnectionFactory<*>;

    fun initialize(credentials : DatabaseCredentials) {
        this.connection = credentials.factory()
    }

    fun connect() : Boolean {
        this.connection.connect()
        return this.connection.isValid();
    }

    fun executor() = this.connection.executor()

    fun close(mode: ShutdownMode) {
        this.connection.close(mode)
    }
}