package dev.httpmarco.polocloud.database.test.mariadb

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MariaDB")
class MariaDbDatabaseTest  : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        TODO("Not yet implemented")
    }

    override fun credentials(): DatabaseCredentials {
        TODO("Not yet implemented")
    }

    companion object {
        @Container
        val mariaDB = MariaDBContainer<Nothing>("mariadb:11.1.0")
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }
    }
}