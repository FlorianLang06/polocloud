package de.polocloud.node.services.factory.task

import de.polocloud.node.services.factory.template.ServiceTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Applies the pre-start configuration tasks of a platform to a service work directory.
 *
 * Flow:
 *  1. The platform references tasks by key + version range ([ServiceTask]).
 *  2. For the concrete service version, only the applicable references are kept.
 *  3. Each referenced [TaskDefinition] is resolved from the loaded definitions and
 *     its [TaskStep]s are applied — the file format is auto-detected from the
 *     extension (`.json` vs. `.properties`).
 *  4. Placeholders such as `%server_port%` in a step value are substituted from the
 *     provided placeholder map before the value is written.
 *
 * Steps are best-effort: a single failing step is logged and skipped so it never
 * blocks a service from starting.
 */
object TaskExecutor {

    private val logger = LoggerFactory.getLogger(TaskExecutor::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Applies all tasks of a platform that match [version] to [workDir].
     *
     * @param workDir      The service work directory whose config files are mutated.
     * @param tasks        Task references declared by the platform.
     * @param version      Concrete platform version of the service being started.
     * @param definitions  Loaded task definitions, keyed by [TaskDefinition.key].
     * @param placeholders Values substituted into `%name%` tokens of step values.
     */
    fun apply(
        workDir: File,
        tasks: List<ServiceTask>,
        version: String,
        definitions: Map<String, TaskDefinition>,
        placeholders: Map<String, String>,
    ) {
        for (task in tasks) {
            if (!task.appliesTo(version)) continue
            val definition = definitions[task.key]
            if (definition == null) {
                logger.warn("Task '{}' is referenced but no definition was loaded — skipping", task.key)
                continue
            }
            for (step in definition.steps) {
                runCatching { applyStep(workDir, step, placeholders) }
                    .onFailure { logger.error("Task '{}' step '{}' failed: {}", task.key, step.name, it.message) }
            }
        }
    }

    /**
     * Applies a single [step], dispatching to the format handler that matches the
     * target file's extension.
     */
    private fun applyStep(workDir: File, step: TaskStep, placeholders: Map<String, String>) {
        val target = File(workDir, step.file)
        target.parentFile?.mkdirs()
        val value = substitutePlaceholders(step.value, placeholders)

        when (target.extension.lowercase()) {
            "properties" -> applyProperties(target, step, value)
            "json" -> applyJson(target, step, value)
            else -> logger.warn(
                "Unsupported task file type '{}' for step '{}' — only .properties and .json are handled",
                target.extension, step.name
            )
        }
        logger.info("  ↳ {} ({} {}={})", step.name, target.name, step.key, value)
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a `key=value` properties file.
     *
     * Existing lines are preserved (comments included): a line whose key matches is
     * rewritten in place, otherwise the entry is appended. A missing file is created
     * — this is the common case, as servers like Spigot only generate
     * `server.properties` on first launch.
     */
    private fun applyProperties(target: File, step: TaskStep, value: String) {
        val newLine = "${step.key}=$value"
        val lines = if (target.exists()) target.readLines().toMutableList() else mutableListOf()

        val index = lines.indexOfFirst { line ->
            val trimmed = line.trimStart()
            !trimmed.startsWith("#") && trimmed.substringBefore('=').trim() == step.key
        }
        if (index >= 0) lines[index] = newLine else lines.add(newLine)

        target.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator())
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a JSON file by setting a top-level [TaskStep.key].
     *
     * A missing or unreadable file is treated as an empty object so the key can still
     * be created. Existing sibling fields are preserved.
     */
    private fun applyJson(target: File, step: TaskStep, value: String) {
        val root = if (target.exists()) {
            runCatching { json.parseToJsonElement(target.readText()).jsonObject }.getOrElse { JsonObject(emptyMap()) }
        } else {
            JsonObject(emptyMap())
        }

        val updated = buildJsonObject {
            root.forEach { (k, v: JsonElement) -> if (k != step.key) put(k, v) }
            put(step.key, JsonPrimitive(value))
        }
        target.writeText(json.encodeToString(JsonObject.serializer(), updated))
    }

    /**
     * Replaces every `%name%` token in [raw] with the matching value from
     * [placeholders]. Unknown tokens are left untouched.
     */
    private fun substitutePlaceholders(raw: String, placeholders: Map<String, String>): String =
        placeholders.entries.fold(raw) { acc, (key, replacement) ->
            acc.replace("%$key%", replacement)
        }
}
