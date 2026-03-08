package de.polocloud.database.test.h2

import de.polocloud.common.Address
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.database.sql.SqlConnectionFactory
import de.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

@DisplayName("H2")
class H2DatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return SqlConnectionFactory(DatabaseCredentials.H2("testdb"))
    }

    companion object {
        @Container
        @JvmStatic
        private val h2 = GenericContainer(DockerImageName.parse("oscarfonts/h2:latest"))
            .withExposedPorts(1521)
            .withEnv("H2_OPTIONS", "-ifNotExists")
    }
}