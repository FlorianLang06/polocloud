package dev.httpmarco.polocloud.database.test.cassandra

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactoryPart
import dev.httpmarco.polocloud.database.sql.SqlDatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.cassandra.CassandraContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("Cassandra")
class CassandraDatabaseTest  : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactoryPart()
    }

    override fun credentials(): DatabaseCredentials {
        return SqlDatabaseCredentials("cassandra", Address(cassandra.host, cassandra.firstMappedPort), "test", "test", "testdb")
    }

    companion object {
        @Container
        @JvmStatic
        val cassandra = CassandraContainer("cassandra:latest").apply {
            withEnv("CASSANDRA_USERNAME", "test")
            withEnv("CASSANDRA_PASSWORD", "test")
        }
    }
}