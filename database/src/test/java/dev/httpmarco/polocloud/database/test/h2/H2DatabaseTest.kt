package dev.httpmarco.polocloud.database.test.h2

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactory
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

@DisplayName("H2")
class H2DatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactory(DatabaseCredentials.H2("h2:tcp", Address(h2.host, h2.firstMappedPort), "test", "test", "testdb"))
    }

    companion object {
        @Container
        @JvmStatic
        private val h2 = GenericContainer(DockerImageName.parse("oscarfonts/h2:latest"))
            .withExposedPorts(1521)
            .withEnv("H2_OPTIONS", "-ifNotExists")
    }
}