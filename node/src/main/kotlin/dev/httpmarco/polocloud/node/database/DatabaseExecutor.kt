package dev.httpmarco.polocloud.node.database

interface DatabaseExecutor {

    fun <T> findAll(key: DatabaseKey<T>): List<T>

    fun <T> update(key: DatabaseKey<T>, value: T)

    fun <T> delete(key: DatabaseKey<T>, value: T)

    fun destroy(key: DatabaseKey<*>);

}