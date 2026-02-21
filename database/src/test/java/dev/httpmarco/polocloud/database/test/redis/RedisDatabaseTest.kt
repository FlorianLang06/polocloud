package dev.httpmarco.polocloud.database.test.redis

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.nosql.redis.RedisConnectionFactory
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("Redis")
class RedisDatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return RedisConnectionFactory(DatabaseCredentials.Redis(Address(redis.host, redis.firstMappedPort), "test", null))
    }

    companion object {
        @Container
        @JvmStatic
        private val redis = GenericContainer("redis:8.2.4-alpine3.22").withExposedPorts(6379)
    }
}