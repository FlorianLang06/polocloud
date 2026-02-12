package dev.httpmarco.polocloud.database.test.redis

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

class RedisDatabaseTest : GeneralDatabaseTest() {

    companion object {
        @Container
        val redis = GenericContainer("redis:7.2.1-alpine").withExposedPorts(6379)
    }

}