package dev.httpmarco.polocloud.database.nosql.redis

import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.database.DatabaseSerializer
import dev.httpmarco.polocloud.database.nosql.AbstractNoSqlExecutor
import redis.clients.jedis.UnifiedJedis

class RedisExecutor(
    private val jedis: UnifiedJedis
) : AbstractNoSqlExecutor() {

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
        val redisKey = "${key.id}:$id"
        val json = jedis.get(redisKey) ?: return null

        return DatabaseSerializer.deserialize(
            json,
            key.clazz
        )
    }

}
