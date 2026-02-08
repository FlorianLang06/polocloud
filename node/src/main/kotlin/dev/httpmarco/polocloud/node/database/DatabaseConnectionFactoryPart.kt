package dev.httpmarco.polocloud.node.database

import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.node.database.credentials.DatabaseCredentials
import dev.httpmarco.polocloud.node.database.sql.SqlExecutor

abstract class DatabaseConnectionFactoryPart<T : DatabaseCredentials> : Closeable {

    val state = DatabaseState.CLOSED

    abstract fun connect(credentials: T)

    abstract fun executor() : SqlExecutor

}