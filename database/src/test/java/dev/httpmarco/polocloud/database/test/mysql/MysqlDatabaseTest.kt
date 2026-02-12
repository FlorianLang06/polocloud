package dev.httpmarco.polocloud.database.test.mysql

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

class MysqlDatabaseTest : GeneralDatabaseTest() {

    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.1.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
}