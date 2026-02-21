package dev.httpmarco.polocloud.database.test.mariadb

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactory
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.mariadb.MariaDBContainer

@DisplayName("MariaDB")
class MariaDbDatabaseTest  : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactory(DatabaseCredentials.MariaDB(Address(mariaDB.host, mariaDB.firstMappedPort), "test", "test", "testdb"))
    }

    companion object {
        @Container
        @JvmStatic
        private val mariaDB = MariaDBContainer("mariadb:noble")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }
    }
}