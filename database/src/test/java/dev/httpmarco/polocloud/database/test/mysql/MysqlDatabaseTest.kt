package dev.httpmarco.polocloud.database.test.mysql

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MySQL")
class MysqlDatabaseTest : GeneralDatabaseTest() {
    override fun factory(): DatabaseConnectionFactory<*> {
        TODO("Not yet implemented")
    }

    override fun credentials(): DatabaseCredentials {
        TODO("Not yet implemented")
    }

    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.1.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
}