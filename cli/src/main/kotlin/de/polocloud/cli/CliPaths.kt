package de.polocloud.cli

import java.io.File

/**
 * Central path management for the CLI.
 *
 * Provides consistent access to all CLI-related directories and files.
 */
object CliPaths {

    /**
     * Root directory for CLI runtime files.
     */
    val ROOT_DIR: File = File("").absoluteFile

    /**
     * Cache directory for temporary and security-related files.
     */
    val CACHE_DIR: File = ROOT_DIR.resolve(".cache")

    /**
     * Logs directory for CLI output files.
     */
    val LOGS_DIR: File = ROOT_DIR.resolve("logs") //TODO

    /**
     * Configuration file path.
     */
    val CONFIG_FILE: File = ROOT_DIR.resolve("polocloud-cli.json")

    /**
     * Installer Configuration file path.
     */
    val INSTALLER_FILE: File = ROOT_DIR.resolve(".installer/config.json")

    init {
        // Ensure required directories exist
        CACHE_DIR.mkdirs()
        LOGS_DIR.mkdirs()
    }
}
