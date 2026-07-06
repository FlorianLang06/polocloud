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
    fun `creates nested yaml path from dotted key with native boolean`(@TempDir dir: File) {
        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "paper_config")),
            version = "1.21.4",
            definitions = mapOf(
                "paper_config" to TaskDefinition(
                    key = "paper_config",
                    steps = listOf(
                        TaskStep(
                            name = "Allow connections over Velocity proxy",
                            file = "config/paper-global.yml",
                            type = TaskStepType.REPLACE,
                            key = "proxies.velocity.enabled",
                            value = "true",
                        )
                    )
                )
            ),
            placeholders = emptyMap(),
        )

        val text = File(dir, "config/paper-global.yml").readText()
        assertTrue(text.contains("proxies:"), text)
        assertTrue(text.contains("velocity:"), text)
        // native boolean, not the quoted string "true"
        assertTrue(Regex("""enabled:\s*true""").containsMatchIn(text), text)
    }

    @Test
    fun `updates nested yaml key and preserves sibling entries`(@TempDir dir: File) {
        val file = File(dir, "config/paper-global.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            proxies:
              velocity:
                enabled: false
                secret: keep-me
            unrelated: value
            """.trimIndent()
        )

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "paper_config")),
            version = "1.21.4",
            definitions = mapOf(
                "paper_config" to TaskDefinition(
                    key = "paper_config",
                    steps = listOf(
                        TaskStep(
                            name = "Allow connections over Velocity proxy",
                            file = "config/paper-global.yml",
                            key = "proxies.velocity.enabled",
                            value = "true",
                        )
                    )
                )
            ),
            placeholders = emptyMap(),
        )

        val text = file.readText()
        assertTrue(Regex("""enabled:\s*true""").containsMatchIn(text), text)
        assertTrue(text.contains("secret: keep-me"), text)
        assertTrue(text.contains("unrelated: value"), text)
    }

    @Test
    fun `substitutes forwarding secret placeholder into nested yaml as a string`(@TempDir dir: File) {
        // an all-digit token must stay a string, never be coerced to a number
        val secret = "12345678901234567890123456789012"

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "paper_config")),
            version = "1.21.4",
            definitions = mapOf(
                "paper_config" to TaskDefinition(
                    key = "paper_config",
                    steps = listOf(
                        TaskStep(
                            name = "Set forwarding secret",
                            file = "config/paper-global.yml",
                            key = "proxies.velocity.secret",
                            value = "%FORWARDING_SECRET%",
                        )
                    )
                )
            ),
            placeholders = mapOf("FORWARDING_SECRET" to secret),
        )

        val file = File(dir, "config/paper-global.yml")
        val yaml = org.yaml.snakeyaml.Yaml().load<Map<String, Any?>>(file.readText())

        @Suppress("UNCHECKED_CAST")
        val velocity = (yaml["proxies"] as Map<String, Any?>)["velocity"] as Map<String, Any?>
        assertEquals(secret, velocity["secret"])
    }

    @Test
    fun `sets top level toml key as quoted string when file is absent`(@TempDir dir: File) {
        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "velocity_config")),
            version = "3.3.0",
            definitions = mapOf(
                "velocity_config" to TaskDefinition(
                    key = "velocity_config",
                    steps = listOf(
                        TaskStep(
                            name = "Setup velocity for best forwarding support",
                            file = "velocity.toml",
                            key = "player-info-forwarding-mode",
                            value = "modern",
                        )
                    )
                )
            ),
            placeholders = emptyMap(),
        )

        val file = File(dir, "velocity.toml")
        assertTrue(file.readText().contains("player-info-forwarding-mode = \"modern\""), file.readText())
        val parsed = com.moandjiezana.toml.Toml().read(file)
        assertEquals("modern", parsed.getString("player-info-forwarding-mode"))
    }

    @Test
    fun `updates toml key and preserves existing tables and siblings`(@TempDir dir: File) {
        val file = File(dir, "velocity.toml")
        file.writeText(
            """
            config-version = "2.7"
            player-info-forwarding-mode = "none"

            [advanced]
            compression-threshold = 256
            """.trimIndent()
        )

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "velocity_config")),
            version = "3.3.0",
            definitions = mapOf(
                "velocity_config" to TaskDefinition(
                    key = "velocity_config",
                    steps = listOf(
                        TaskStep(
                            name = "Setup velocity for best forwarding support",
                            file = "velocity.toml",
                            key = "player-info-forwarding-mode",
                            value = "modern",
                        )
                    )
                )
            ),
            placeholders = emptyMap(),
        )

        val parsed = com.moandjiezana.toml.Toml().read(file)
        assertEquals("modern", parsed.getString("player-info-forwarding-mode"))
        assertEquals("2.7", parsed.getString("config-version"))
        assertEquals(256L, parsed.getLong("advanced.compression-threshold"))
    }

    @Test
    fun `writes raw value as whole file content when key is null`(@TempDir dir: File) {
        val secret = "abc123def456"

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "velocity_secret")),
            version = "1.21.4",
            definitions = mapOf(
                "velocity_secret" to TaskDefinition(
                    key = "velocity_secret",
                    steps = listOf(
                        TaskStep(
                            name = "Setup right forwarding secret",
                            file = "forwarding.secret",
                            key = null,
                            value = "%FORWARDING_SECRET%",
                        )
                    )
                )
            ),
            placeholders = mapOf("FORWARDING_SECRET" to secret),
        )

        assertEquals(secret, File(dir, "forwarding.secret").readText())
    }

    @Test
    fun `creates nested json path from dotted key`(@TempDir dir: File) {
        File(dir, "config.json").writeText("""{"keep":"me"}""")

        TaskExecutor.apply(
            workDir = dir,
            tasks = listOf(ServiceTask(key = "json_task")),
            version = "1.0",
            definitions = mapOf(
                "json_task" to TaskDefinition(
                    key = "json_task",
                    steps = listOf(
                        TaskStep(
                            name = "enable velocity",
                            file = "config.json",
                            key = "proxies.velocity.enabled",
                            value = "true",
                        )
                    )
                )
            ),
            placeholders = emptyMap(),
        )

        val text = File(dir, "config.json").readText()
        assertTrue(text.contains("\"proxies\""), text)
        assertTrue(text.contains("\"velocity\""), text)
        assertTrue(text.contains("\"enabled\": \"true\""), text)
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
