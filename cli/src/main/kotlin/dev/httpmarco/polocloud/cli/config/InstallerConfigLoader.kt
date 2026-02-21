package dev.httpmarco.polocloud.cli.config

import dev.httpmarco.polocloud.cli.logger
import kotlinx.serialization.json.Json
import java.io.File

object InstallerConfigLoader {

    private val configFile = File(".installer/config.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private var cached: InstallerConfig? = null

    fun load(): InstallerConfig {
        cached?.let { return it }

        val config = try {
            if (!configFile.exists()) {
                logger.warn("Installer config not found at ${configFile.path}. Using defaults.")
                InstallerConfig()
            } else {
                json.decodeFromString<InstallerConfig>(
                    configFile.readText()
                )
            }
        } catch (ex: Exception) {
            logger.error("Failed to parse installer config. Using defaults.", ex)
            InstallerConfig() // 🔥 Fallback
        }

        cached = config
        return config
    }
}