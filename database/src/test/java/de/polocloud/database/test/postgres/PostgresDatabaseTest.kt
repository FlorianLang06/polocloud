package de.polocloud.database.test.postgres

import de.polocloud.common.Address
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.database.sql.SqlConnectionFactory
import de.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer

@DisplayName("PostgreSQL")
internal class PostgresDatabaseTest  : GeneralDatabaseTest() {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactory(DatabaseCredentials.PostgreSQL(Address(postgres.host, postgres.firstMappedPort), "test", "test", "testdb"))
    }
}