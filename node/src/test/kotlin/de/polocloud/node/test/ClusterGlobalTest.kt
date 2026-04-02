package de.polocloud.node.test

import de.polocloud.common.Address
import de.polocloud.common.LOCALHOST_ADDRESS
import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.files.deleteComplete
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.NodeInstance
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.configuration.GeneralConfiguration
import de.polocloud.node.configuration.LocalNodeConfiguration
import de.polocloud.node.configuration.NodeConfigurations
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
import kotlin.random.Random

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

        val amount = Random.nextInt(2, 10)
        var behaviorNode : NodeInstance? = null

        for (i in 0 until amount) {
            val address = LOCALHOST_ADDRESS.withPort(5600 + i)
            val testingDir = Path("testing-${UUID.randomUUID()}")

            System.setProperty("rootDir", testingDir.toString())

            val launchProperties = NodeLaunchProperties(
                rootDir = testingDir,
                address = address,
                clusterRegistration = if(behaviorNode == null) null
                else RegistrationInfo(
                    behaviorNode.registrationManager.publicRegistrationToken,
                    behaviorNode.configurations.clusterConfig.registration
                )
            )

            val localNodeHolder = ConfigurationManager
                .load<LocalNodeConfiguration>()
                .atPath(testingDir.resolve("local-node.json").toString())

            val clusterHolder = ConfigurationManager
                .load<ClusterConfiguration>()
                .atPath(testingDir.resolve("cluster.json").toString())

            val generalHolder = ConfigurationManager
                .load<GeneralConfiguration>()
                .atPath(testingDir.resolve("general.json").toString())

            localNodeHolder.value = LocalNodeConfiguration( //TODO set to database configuration with envs
                DatabaseCredentials.PostgreSQL(
                    Address(postgres.host, postgres.firstMappedPort),
                    "test",
                    "test",
                    "testdb"
                )
            )

            clusterHolder.mutate {
                registration = Address(registration.hostname, 6600 + i)
            }

            val configurations = NodeConfigurations(
                cluster = clusterHolder,
                general = generalHolder,
                localNode = localNodeHolder,
            )

            val nodeInstance = NodeInstance(
                launchProperties,
                configurations
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
            it.launchProperties.rootDir.deleteComplete()
        }
    }
}