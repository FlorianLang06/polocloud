package dev.httpmarco.polocloud.database.test.mariadb

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactoryPart
import dev.httpmarco.polocloud.database.sql.SqlDatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import dev.httpmarco.polocloud.database.test.postgres.PostgresDatabaseTest.Companion.postgres
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DisplayName("MariaDB")
class MariaDbDatabaseTest  : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactoryPart()
    }

    override fun credentials(): DatabaseCredentials {
        return SqlDatabaseCredentials("mariadb", Address(mariaDB.host, mariaDB.firstMappedPort), "test", "test", "testdb")
    }

    companion object {
        @Container
        @JvmStatic
        private val mariaDB = MariaDBContainer<Nothing>("mariadb:noble")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }
    }
}