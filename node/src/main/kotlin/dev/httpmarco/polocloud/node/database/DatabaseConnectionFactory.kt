package dev.httpmarco.polocloud.node.database

import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.node.database.credentials.DatabaseCredentials
import dev.httpmarco.polocloud.node.database.sql.SqlExecutor

abstract class DatabaseConnectionFactory<T : DatabaseCredentials> : Closeable {

    val state = DatabaseState.UNKNOWN

    abstract fun connect(credentials: T)

    abstract fun executor() : SqlExecutor

}