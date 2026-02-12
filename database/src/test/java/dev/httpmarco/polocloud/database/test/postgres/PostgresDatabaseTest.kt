package dev.httpmarco.polocloud.database.test.postgres

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

class PostgresDatabaseTest  : GeneralDatabaseTest() {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
}