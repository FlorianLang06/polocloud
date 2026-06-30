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
 * The target file format is detected from its extension (`.json` vs. `.properties`),
 * so the same step model works across both layouts: [key] is filtered inside the
 * file and its [value] is set according to [type].
 *
 * @param name  Human-readable description of what the step does.
 * @param file  Relative path (to the service work directory) of the file to edit.
 * @param type  How the value is applied. Currently only [TaskStepType.REPLACE].
 * @param key   The property/field key to locate inside [file].
 * @param value New value to set. May contain placeholders such as `%server_port%`.
 */
@Serializable
data class TaskStep(
    val name: String,
    val file: String,
    val type: TaskStepType = TaskStepType.REPLACE,
    val key: String,
    val value: String
)

/**
 * Strategy describing how a [TaskStep] applies its value to a key.
 *
 * Currently only [REPLACE] exists: the existing value of the key is overwritten
 * (the key is created if it does not yet exist).
 */
@Serializable
enum class TaskStepType {
    REPLACE
}
