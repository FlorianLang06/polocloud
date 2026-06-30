package de.polocloud.node.services.factory.task

import de.polocloud.node.services.factory.template.ServiceTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TaskExecutorTest {

    private val serverPropertiesTask = TaskDefinition(
        key = "server_properties",
        steps = listOf(
            TaskStep(
                name = "Setup port for spigot based server",
                file = "server.properties",
                type = TaskStepType.REPLACE,
                key = "server-port",
                value = "%server_port%",
            )
        )
    )

    @Test
    fun `creates properties file and substitutes placeholder when file is absent`(@TempDir dir: File) {
        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "server_properties", from = "1.7", until = "26.2")),
            version = "1.21.4",
            definitions = mapOf("server_properties" to serverPropertiesTask),
            placeholders = mapOf("server_port" to "25570"),
        )

        val props = File(dir, "server.properties").readLines()
        assertTrue(props.contains("server-port=25570"))
    }

    @Test
    fun `replaces existing key and preserves comments and other entries`(@TempDir dir: File) {
        File(dir, "server.properties").writeText(
            """
            #Minecraft server properties
            server-port=25565
            motd=A Minecraft Server
            """.trimIndent()
        )

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "server_properties")),
            version = "1.21.4",
            definitions = mapOf("server_properties" to serverPropertiesTask),
            placeholders = mapOf("server_port" to "25571"),
        )

        val props = File(dir, "server.properties").readLines()
        assertTrue(props.contains("server-port=25571"))
        assertTrue(props.contains("motd=A Minecraft Server"))
        assertTrue(props.any { it.startsWith("#") })
        assertEquals(1, props.count { it.startsWith("server-port=") })
    }

    @Test
    fun `detects json layout from extension and sets top level key`(@TempDir dir: File) {
        File(dir, "config.json").writeText("""{"keep":"me","server-port":"0"}""")

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "json_task")),
            version = "1.0",
            definitions = mapOf(
                "json_task" to TaskDefinition(
                    key = "json_task",
                    steps = listOf(
                        TaskStep(
                            name = "set port",
                            file = "config.json",
                            key = "server-port",
                            value = "%server_port%",
                        )
                    )
                )
            ),
            placeholders = mapOf("server_port" to "30000"),
        )

        val text = File(dir, "config.json").readText()
        assertTrue(text.contains("\"server-port\": \"30000\""), text)
        assertTrue(text.contains("\"keep\": \"me\""), text)
    }

    @Test
    fun `skips tasks outside the version range`(@TempDir dir: File) {
        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "server_properties", from = "1.7", until = "1.20")),
            version = "1.21.4",
            definitions = mapOf("server_properties" to serverPropertiesTask),
            placeholders = mapOf("server_port" to "25570"),
        )

        assertEquals(false, File(dir, "server.properties").exists())
    }
}
