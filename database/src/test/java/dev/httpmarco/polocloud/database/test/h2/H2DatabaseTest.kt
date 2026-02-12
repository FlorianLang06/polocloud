package dev.httpmarco.polocloud.database.test.h2

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

class H2DatabaseTest : GeneralDatabaseTest() {

    companion object {
        @Container
        val h2Container = GenericContainer(DockerImageName.parse("oscarfonts/h2:latest")).withExposedPorts(1521)
    }

}