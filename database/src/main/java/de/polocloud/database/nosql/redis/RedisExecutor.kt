package de.polocloud.database.nosql.redis

import de.polocloud.database.DatabaseKey
import de.polocloud.database.DatabaseSerializer
import de.polocloud.database.filtering.Filter
import de.polocloud.database.nosql.AbstractNoSqlExecutor
import redis.clients.jedis.UnifiedJedis

class RedisExecutor(
    private val jedis: UnifiedJedis
) : AbstractNoSqlExecutor() {

    private val filterTranslator = RedisTranslator()

    override fun write(collection: String, identifier: String, json: String) {
        jedis.set("$collection:$identifier", json)
    }

    override fun readAll(collection: String): List<String> {
        val keys = jedis.keys("$collection:*")
        return keys.mapNotNull { jedis.get(it) }
    }

    override fun deleteInternal(collection: String, identifier: String) {
        jedis.del("$collection:$identifier")
    }

    override fun existsInternal(collection: String, identifier: String): Boolean {
        return jedis.exists("$collection:$identifier")
    }

    override fun destroyInternal(collection: String) {
        val keys = jedis.keys("$collection:*")
        if (keys.isNotEmpty()) {
            jedis.del(*keys.toTypedArray())
        }
    }

    override fun <T : Any> findById(key: DatabaseKey<T>, id: Any): T? {
        val redisKey = "${key.id()}:$id"
        val json = jedis.get(redisKey) ?: return null

        return DatabaseSerializer.deserialize(
            json,
            key.clazz
        )
    }

    override fun <T : Any> find(
        key: DatabaseKey<T>,
        vararg filters: Filter
    ): List<T> {

        val all = readAll(key.id()).map {
            DatabaseSerializer.deserialize(it, key.clazz)
        }

        if (filters.isEmpty()) return all

        // hier müsstest du selbst filtern
        return all
    }

    override fun filterTranslator() = filterTranslator

}
