package dev.httpmarco.polocloud.node.database.sql

import dev.httpmarco.polocloud.node.database.DatabaseExecutor
import dev.httpmarco.polocloud.node.database.DatabaseKey

class SqlExecutor(val factory : SqlConnectionFactoryPart) : DatabaseExecutor {

    override fun <T> findAll(key: DatabaseKey<T>): List<T> {
        return emptyList()
    }

    override fun <T> update(key: DatabaseKey<T>, value: T) {

    }

    override fun <T> delete(key: DatabaseKey<T>, value: T) {

    }

    override fun destroy(key: DatabaseKey<*>) {

    }
}