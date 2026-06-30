package de.polocloud.node.services.factory.task

import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/**
 * Directory name (relative to the platform cache) that holds the task definition files.
 */
const val TASK_DIRECTORY = "tasks"

/**
 * Loads all [TaskDefinition]s from JSON files found in `<cacheDir>/tasks`.
 *
 * Each file is expected to contain a single task definition. Files that fail to
 * deserialize are silently skipped. The result is keyed by [TaskDefinition.key]
 * so platforms can resolve their referenced tasks in O(1); on duplicate keys the
 * last file wins.
 *
 * @param cacheDir Root directory of the platform cache (the `tasks/` folder lives inside it).
 * @return Map of task key → definition.
 */
fun loadTaskDefinitionsFromCache(cacheDir: File = File(".cache/platforms")): Map<String, TaskDefinition> {
    val taskDir = File(cacheDir, TASK_DIRECTORY)
    if (!taskDir.exists() || !taskDir.isDirectory) return emptyMap()
    return taskDir.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .mapNotNull { runCatching { json.decodeFromString<TaskDefinition>(it.readText()) }.getOrNull() }
        .associateBy { it.key }
}
