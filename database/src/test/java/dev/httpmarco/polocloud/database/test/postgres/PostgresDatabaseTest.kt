package dev.httpmarco.polocloud.database.test.postgres

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactoryPart
import dev.httpmarco.polocloud.database.sql.SqlDatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DisplayName("PostgreSQL")
internal class PostgresDatabaseTest  : GeneralDatabaseTest() {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactoryPart()
    }

    override fun credentials(): DatabaseCredentials {
        return SqlDatabaseCredentials("postgresql", Address(postgres.host, postgres.firstMappedPort), "test", "test", "testdb")
    }
}