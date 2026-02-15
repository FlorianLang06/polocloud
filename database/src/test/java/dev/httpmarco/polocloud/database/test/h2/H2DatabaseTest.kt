package dev.httpmarco.polocloud.database.test.h2

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

@DisplayName("H2")
class H2DatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        TODO("Not yet implemented")
    }

    override fun credentials(): DatabaseCredentials {
        TODO("Not yet implemented")
    }

    companion object {
        @Container
        val h2Container = GenericContainer(DockerImageName.parse("oscarfonts/h2:latest")).withExposedPorts(1521)
    }
}