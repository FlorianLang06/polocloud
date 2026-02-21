package dev.httpmarco.polocloud.database.nosql.redis

import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.DatabaseState
import redis.clients.jedis.UnifiedJedis

class RedisConnectionFactory(credentials: DatabaseCredentials.Redis) : DatabaseConnectionFactory<DatabaseCredentials.Redis>(credentials) {

    private lateinit var jedis: UnifiedJedis
    private lateinit var executor: RedisExecutor

    override fun connect(credentials: DatabaseCredentials.Redis) {

        state = DatabaseState.CONNECTING

        val uri = if (credentials.password != null && credentials.password!!.isNotBlank()) {
            "redis://:${credentials.password}@" + credentials.address.asString()
        } else {
            "redis://${credentials.address.asString()}"
        }

        jedis = UnifiedJedis(uri)

        executor = RedisExecutor(jedis)

        state = DatabaseState.CONNECTED
    }

    override fun executor() = executor

    override fun close(mode: ShutdownMode) {
        if (state == DatabaseState.CLOSED) {
            logger.warn("Redis connection already closed")
            return
        }

        try {
            jedis.close()
            state = DatabaseState.CLOSED
            logger.info("Redis connection closed (mode={})", mode)
        } catch (e: Exception) {
            logger.error("Failed to close Redis connection", e)
        }
    }
}
