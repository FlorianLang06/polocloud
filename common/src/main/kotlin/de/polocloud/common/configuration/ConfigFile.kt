package de.polocloud.common.configuration

/**
 * Marks a data class as a configuration.
 * The file path is resolved relative to the working directory.
 *
 * @param path Path to the JSON config file (e.g. "config.json" or "configs/server.json")
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigFile(val path: String)
