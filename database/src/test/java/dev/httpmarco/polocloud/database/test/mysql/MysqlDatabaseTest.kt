package dev.httpmarco.polocloud.database.test.mysql

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactoryPart
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MySQL")
class MysqlDatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactoryPart()
    }

    override fun credentials(): DatabaseCredentials {
        return SqlDatabaseCredentials("mysql", Address(mysql.host, mysql.firstMappedPort), "test", "test", "testdb")
    }

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer<Nothing>("mysql:8.0.33").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withEnv("MYSQL_ROOT_PASSWORD", "test")
        }
    }
}