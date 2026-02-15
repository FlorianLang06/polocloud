package dev.httpmarco.polocloud.database.test.redis

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("Redis")
class RedisDatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        TODO("Not yet implemented")
    }

    override fun credentials(): DatabaseCredentials {
        TODO("Not yet implemented")
    }

    companion object {
        @Container
        val redis = GenericContainer("redis:8.2.4-alpine3.22").withExposedPorts(6379)
    }
}