package de.polocloud.database.test.mysql

import de.polocloud.common.Address
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.database.sql.SqlConnectionFactory
import de.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MySQL")
class MysqlDatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactory(DatabaseCredentials.Mysql(Address(mysql.host, mysql.firstMappedPort), "test", "test", "testdb"))
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