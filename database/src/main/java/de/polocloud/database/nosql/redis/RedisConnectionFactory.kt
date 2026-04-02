package de.polocloud.database.nosql.redis

import de.polocloud.common.ShutdownMode
import de.polocloud.common.i18n.trError
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.i18n.trWarn
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.database.DatabaseState
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
            logger.trWarn("database", "database.connection.already_closed")
            return
        }

        try {
            jedis.close()
            state = DatabaseState.CLOSED
            logger.trInfo("database","database.connection.closed.with_mode","mode" to mode)
        } catch (e: Exception) {
            logger.trError("database","database.connection.close.failed", e)
        }
    }
}
