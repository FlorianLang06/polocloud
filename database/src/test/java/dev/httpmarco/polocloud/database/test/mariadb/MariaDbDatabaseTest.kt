package dev.httpmarco.polocloud.database.test.mariadb

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

class MariaDbDatabaseTest  : GeneralDatabaseTest() {

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