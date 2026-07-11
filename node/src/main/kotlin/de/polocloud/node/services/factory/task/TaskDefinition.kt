package de.polocloud.node.services.factory.task

import kotlinx.serialization.Serializable

/**
 * A reusable, named task that mutates one or more configuration files of a service
 * before it is started. Definitions are shipped alongside the platform templates
 * (under `tasks/` in the platform cache) and referenced from a platform via
 * [de.polocloud.node.services.factory.template.ServiceTask.key].
 *
 * Example (`tasks/server_properties.json`):
 * ```json
 * {
 *   "key": "server_properties",
 *   "steps": [
 *     {
 *       "name": "Setup port for spigot based server",
 *       "file": "server.properties",
 *       "type": "REPLACE",
 *       "key": "server-port",
 *       "value": "%server_port%"
 *     }
 *   ]
 * }
 * ```
 *
 * @param key   Unique identifier matching the referencing [ServiceTask.key].
 * @param steps Ordered list of mutations applied to the service work directory.
 */
@Serializable
data class TaskDefinition(
    val key: String,
    val steps: List<TaskStep> = emptyList()
)

/**
 * A single mutation applied to a configuration [file] of a service.
 *
 * The target file format is detected from its extension (`.json`, `.yml`/`.yaml`,
 * `.toml` or `.properties`), so the same step model works across every layout: [key]
 * is located inside the file and its [value] is set according to [type].
 *
 * For the structured formats (`.json`, `.yml`/`.yaml`, `.toml`) [key] may address a
 * nested field using `.` as a path separator, e.g. `proxies.velocity.enabled`; missing
 * intermediate objects are created. In `.properties` files the `.` is part of the
 * literal key.
 *
 * When [key] is `null` the file is treated as a single opaque value: [value] becomes
 * the entire file content (regardless of extension). This is used for bare files such
 * as a `forwarding.secret` that only hold one token.
 *
 * Example (`tasks/paper_config.json`):
 * ```json
 * {
 *   "key": "paper_config",
 *   "steps": [
 *     {
 *       "name": "Allow connections over Velocity proxy",
 *       "file": "config/paper-global.yml",
 *       "type": "REPLACE",
 *       "key": "proxies.velocity.enabled",
 *       "value": "true"
 *     }
 *   ]
 * }
 * ```
 *
 * @param name  Human-readable description of what the step does.
 * @param file  Relative path (to the service work directory) of the file to edit. For
 *              [TaskStepType.COPY_FILE_IF_NOT_EXISTS] this same relative path is also
 *              used to locate the source file inside the platform's cache directory.
 * @param type  How the step is applied, see [TaskStepType].
 * @param key   The property/field key to locate inside [file]. For structured formats
 *              this may be a `.`-separated path into nested objects. When `null`, the
 *              whole file content is replaced with [value]. Unused for
 *              [TaskStepType.COPY_FILE_IF_NOT_EXISTS].
 * @param value New value to set. May contain placeholders such as `%server_port%`.
 *              Unused for [TaskStepType.COPY_FILE_IF_NOT_EXISTS].
 */
@Serializable
data class TaskStep(
    val name: String,
    val file: String,
    val type: TaskStepType = TaskStepType.REPLACE,
    val key: String? = null,
    val value: String = ""
)

/**
 * Strategy describing how a [TaskStep] is applied.
 */
@Serializable
enum class TaskStepType {
    /** The existing value of [TaskStep.key] is overwritten (created if absent). */
    REPLACE,

    /**
     * Copies [TaskStep.file] from the platform's cache directory into the service work
     * directory, but only if no file already sits at that path. Used to seed a config
     * file (e.g. `velocity.toml`) that a template did not already provide, without
     * clobbering layout changes an operator made to an existing one.
     */
    COPY_FILE_IF_NOT_EXISTS
}
