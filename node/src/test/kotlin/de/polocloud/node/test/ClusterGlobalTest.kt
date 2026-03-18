package de.polocloud.node.test

import de.polocloud.common.Address
import de.polocloud.common.LOCALHOST_ADDRESS
import de.polocloud.common.ShutdownMode
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.NodeInstance
import de.polocloud.node.configuration.NodeConfiguration
import de.polocloud.node.launch.NodeLaunchProperties
import de.polocloud.node.registration.RegistrationInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID
import kotlin.io.path.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClusterGlobalTest {

    private val nodeList = arrayListOf<NodeInstance>()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true)
        }
    }

    @BeforeAll
    fun setup() {
        postgres.start()
    }

    @Test
    fun test() {

        val amount = 2
        var behaviorNode : NodeInstance? = null

        for (i in 0 until amount) {
            val address = LOCALHOST_ADDRESS.withPort(5600 + i)
            val launchProperties = NodeLaunchProperties(
                rootDir = Path("testing-${UUID.randomUUID()}"),
                address = address,
                clusterRegistration = if(behaviorNode == null) null else RegistrationInfo(behaviorNode.registrationManager.publicRegistrationToken, behaviorNode.nodeConfig.cluster.registration)
            )

            val nodeInstance = NodeInstance(
                launchProperties, NodeConfiguration(
                    DatabaseCredentials.PostgreSQL(
                        Address(postgres.host, postgres.firstMappedPort),
                        "test",
                        "test",
                        "testdb"
                    )
                )
            )

            behaviorNode = nodeInstance
            nodeList.add(nodeInstance)

            nodeInstance.start()
        }
    }

    @AfterAll
    fun tearDown() {
        nodeList.forEach {
            it.close(ShutdownMode.GRACEFUL)
        }
    }
}