package de.polocloud.node.services.factory.task

import de.polocloud.node.services.factory.template.ServiceTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Applies the pre-start configuration tasks of a platform to a service work directory.
 *
 * Flow:
 *  1. The platform references tasks by key + version range ([ServiceTask]).
 *  2. For the concrete service version, only the applicable references are kept.
 *  3. Each referenced [TaskDefinition] is resolved from the loaded definitions and
 *     its [TaskStep]s are applied — the file format is auto-detected from the
 *     extension (`.json`, `.yml`/`.yaml`, `.toml` or `.properties`).
 *  4. Placeholders such as `%server_port%` in a step value are substituted from the
 *     provided placeholder map before the value is written.
 *
 * For structured formats (`.json`, `.yml`/`.yaml`, `.toml`) a [TaskStep.key] may
 * address a nested field using `.` as a path separator, e.g. `proxies.velocity.enabled`
 * creates/updates `enabled` inside `velocity` inside `proxies`. In `.properties`
 * files the dot is part of the literal key, as usual for that format.
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
     * @param platformDir  The platform's own cache directory (e.g. `.cache/platforms/velocity`),
     *                     used as the source for [TaskStepType.COPY_FILE_IF_NOT_EXISTS] steps.
     */
    fun apply(
        workDir: File,
        tasks: List<ServiceTask>,
        version: String,
        definitions: Map<String, TaskDefinition>,
        placeholders: Map<String, String>,
        platformDir: File = workDir,
    ) {
        for (task in tasks) {
            if (!task.appliesTo(version)) continue
            val definition = definitions[task.key]
            if (definition == null) {
                logger.warn("Task '{}' is referenced but no definition was loaded — skipping", task.key)
                continue
            }
            for (step in definition.steps) {
                runCatching { applyStep(workDir, platformDir, step, placeholders) }
                    .onFailure { logger.error("Task '{}' step '{}' failed: {}", task.key, step.name, it.message) }
            }
        }
    }

    /**
     * Applies a single [step].
     *
     * [TaskStepType.COPY_FILE_IF_NOT_EXISTS] copies [TaskStep.file] from [platformDir]
     * into the work directory, skipping it if the target already exists. Otherwise a
     * keyless step ([TaskStep.key] `null`) writes [TaskStep.value] as the whole file
     * content, and a keyed step is dispatched to the format handler that matches the
     * target file's extension.
     */
    private fun applyStep(workDir: File, platformDir: File, step: TaskStep, placeholders: Map<String, String>) {
        val target = File(workDir, step.file)
        target.parentFile?.mkdirs()

        if (step.type == TaskStepType.COPY_FILE_IF_NOT_EXISTS) {
            copyIfNotExists(platformDir, target, step)
            return
        }

        val value = substitutePlaceholders(step.value, placeholders)

        val key = step.key
        if (key == null) {
            target.writeText(value)
            logger.info("  &8↳ {} ({})", step.name, target.name)
            return
        }

        when (target.extension.lowercase()) {
            "properties" -> applyProperties(target, key, value)
            "json" -> applyJson(target, key, value)
            "yml", "yaml" -> applyYaml(target, key, value)
            "toml" -> applyToml(target, key, value)
            else -> logger.warn(
                "Unsupported task file type '{}' for step '{}' — only .properties, .json, .yml/.yaml and .toml are handled",
                target.extension, step.name
            )
        }
        logger.info("  ↳ {} ({} {}={})", step.name, target.name, key, value)
    }

    /**
     * Applies a [TaskStepType.COPY_FILE_IF_NOT_EXISTS] step: copies [step]'s file from
     * [platformDir] to [target], but only if [target] does not already exist — an
     * operator's existing layout is never overwritten. A missing source file is logged
     * and skipped rather than failing the whole task.
     */
    private fun copyIfNotExists(platformDir: File, target: File, step: TaskStep) {
        if (target.exists()) {
            logger.info("  &8↳ {} ({} already present, skipped)", step.name, target.name)
            return
        }
        val source = File(platformDir, step.file)
        if (!source.exists()) {
            logger.warn("Task step '{}' cannot copy '{}' — source not found in platform cache", step.name, source.path)
            return
        }
        source.copyTo(target)
        logger.info("  ↳ {} ({} copied from platform cache)", step.name, target.name)
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a `key=value` properties file.
     *
     * Existing lines are preserved (comments included): a line whose key matches is
     * rewritten in place, otherwise the entry is appended. A missing file is created
     * — this is the common case, as servers like Spigot only generate
     * `server.properties` on first launch.
     */
    private fun applyProperties(target: File, key: String, value: String) {
        val newLine = "$key=$value"
        val lines = if (target.exists()) target.readLines().toMutableList() else mutableListOf()

        val index = lines.indexOfFirst { line ->
            val trimmed = line.trimStart()
            !trimmed.startsWith("#") && trimmed.substringBefore('=').trim() == key
        }
        if (index >= 0) lines[index] = newLine else lines.add(newLine)

        target.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator())
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a JSON file by setting the [TaskStep.key].
     *
     * The key may be a `.`-separated path (e.g. `proxies.velocity.enabled`); missing
     * intermediate objects are created. A missing or unreadable file is treated as an
     * empty object so the key can still be created, and existing sibling fields are
     * preserved. Values are always written as JSON strings.
     */
    private fun applyJson(target: File, key: String, value: String) {
        val root = if (target.exists()) {
            runCatching { json.parseToJsonElement(target.readText()).jsonObject }.getOrElse { JsonObject(emptyMap()) }
        } else {
            JsonObject(emptyMap())
        }

        val updated = setNestedJson(root, keyPath(key), JsonPrimitive(value))
        target.writeText(json.encodeToString(JsonObject.serializer(), updated))
    }

    /**
     * Returns a copy of [obj] with [path] set to [value], creating any missing
     * intermediate objects and preserving every sibling field along the way.
     */
    private fun setNestedJson(obj: JsonObject, path: List<String>, value: JsonElement): JsonObject {
        val head = path.first()
        return buildJsonObject {
            obj.forEach { (k, v: JsonElement) -> if (k != head) put(k, v) }
            if (path.size == 1) {
                put(head, value)
            } else {
                val child = obj[head] as? JsonObject ?: JsonObject(emptyMap())
                put(head, setNestedJson(child, path.drop(1), value))
            }
        }
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a YAML file by setting the [TaskStep.key].
     *
     * As with JSON, the key may be a `.`-separated path (e.g.
     * `proxies.velocity.enabled`) and missing intermediate maps are created. A missing
     * file is treated as an empty document. Scalar values that look like a boolean or
     * number are written as their native YAML type so consumers (e.g. Paper) read
     * `enabled: true`, not `enabled: "true"`.
     *
     * An existing file that fails to parse is left untouched (step skipped, logged)
     * rather than replaced by an empty document, so a malformed file never loses every
     * other key it already held.
     *
     * Note: comments in an existing file are not preserved — the document is
     * re-serialized. In practice these files are generated by the server on first
     * launch, so the task typically writes a fresh partial file that the server merges.
     */
    private fun applyYaml(target: File, key: String, value: String) {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        val yaml = Yaml(options)

        val root: MutableMap<String, Any?> = if (target.exists()) {
            @Suppress("UNCHECKED_CAST")
            val parsed = runCatching { yaml.load<Any?>(target.readText()) }
            if (parsed.isFailure) {
                logger.error(
                    "Failed to parse existing YAML file '{}' — leaving it untouched: {}",
                    target.path, parsed.exceptionOrNull()?.message
                )
                return
            }
            // A blank file or one holding a bare scalar/list parses successfully but
            // isn't a map — nothing to lose, so it's simply treated as an empty document.
            (parsed.getOrNull() as? MutableMap<String, Any?>) ?: linkedMapOf()
        } else {
            linkedMapOf()
        }

        setNestedMap(root, keyPath(key), coerceScalar(value))
        target.writeText(yaml.dump(root))
    }

    /**
     * Applies a [TaskStepType.REPLACE] to a TOML file (e.g. Velocity's `velocity.toml`)
     * by setting the [key], which may be a `.`-separated path into nested tables.
     *
     * Like the YAML handler this loads the document into a map, sets the value and
     * re-serializes it; a missing file starts from an empty document. Scalars are
     * written with their native TOML type, so a string stays quoted
     * (`player-info-forwarding-mode = "modern"`) while booleans/integers are unquoted.
     *
     * An existing file that fails to parse is left untouched (step skipped, logged) —
     * never replaced by an empty document, since that would silently discard every
     * other key already in the file (e.g. `bind`, `[servers]`) and break the platform
     * on its next start. Parsing can fail on a file this same method wrote earlier: a
     * quoted, dotted TOML key (e.g. `"lobby.example.com"` in Velocity's
     * `[forced-hosts]`) is read back by toml4j with its quotes still part of the key
     * string, so [unquoteTomlKeys] strips them again after every read to keep the key
     * re-writable instead of accumulating an extra quote layer each round-trip.
     *
     * Note: comments in an existing file are not preserved. In practice `velocity.toml`
     * is generated by the proxy on first launch, so the task writes a fresh partial file
     * that the proxy merges with its defaults.
     */
    private fun applyToml(target: File, key: String, value: String) {
        val root: MutableMap<String, Any?> = if (target.exists()) {
            val parsed = runCatching { Toml().read(target).toMap() as MutableMap<String, Any?> }
            val map = parsed.getOrNull()
            if (map == null) {
                logger.error(
                    "Failed to parse existing TOML file '{}' — leaving it untouched: {}",
                    target.path, parsed.exceptionOrNull()?.message
                )
                return
            }
            unquoteTomlKeys(map)
            map
        } else {
            linkedMapOf()
        }

        setNestedMap(root, keyPath(key), coerceScalar(value))
        target.writeText(TomlWriter().write(root))
    }

    /**
     * Strips a single surrounding pair of `"` or `'` from every map key in [map],
     * recursing into nested tables. toml4j keeps the quote characters of a quoted TOML
     * key (e.g. `"lobby.example.com"`) as part of the key string returned by
     * [Toml.toMap]; left as-is, [TomlWriter] would quote it *again* on write, and the
     * doubly-quoted key then fails to parse the next time the file is read.
     */
    @Suppress("UNCHECKED_CAST")
    private fun unquoteTomlKeys(map: MutableMap<String, Any?>) {
        val entries = map.entries.toList()
        for ((rawKey, v) in entries) {
            if (v is MutableMap<*, *>) unquoteTomlKeys(v as MutableMap<String, Any?>)
            val unquoted = unquoteTomlKey(rawKey)
            if (unquoted != rawKey) {
                map.remove(rawKey)
                map[unquoted] = v
            }
        }
    }

    private fun unquoteTomlKey(key: String): String =
        if (key.length >= 2 && ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'")))) {
            key.substring(1, key.length - 1)
        } else {
            key
        }

    /**
     * Sets [path] to [value] inside [map], creating any missing intermediate maps and
     * preserving sibling entries. A non-map value encountered mid-path is replaced by a
     * fresh map so the remaining path can be written. Shared by the YAML and TOML
     * handlers, which both model a document as nested maps.
     */
    private fun setNestedMap(map: MutableMap<String, Any?>, path: List<String>, value: Any?) {
        val head = path.first()
        if (path.size == 1) {
            map[head] = value
            return
        }
        @Suppress("UNCHECKED_CAST")
        val child = map[head] as? MutableMap<String, Any?> ?: linkedMapOf<String, Any?>().also { map[head] = it }
        setNestedMap(child, path.drop(1), value)
    }

    /**
     * Splits a [TaskStep.key] into its `.`-separated path segments. Blank segments
     * (e.g. from a leading/trailing dot) are dropped; a key without a dot yields a
     * single-element path.
     */
    private fun keyPath(key: String): List<String> =
        key.split('.').filter { it.isNotBlank() }.ifEmpty { listOf(key) }

    /**
     * Converts a string [value] to a native boolean or integer when it unambiguously
     * represents one, so structured formats keep the intended type (e.g. Paper reads
     * `enabled: true`, not `"true"`). Everything else — including secrets and any value
     * that does not round-trip as a canonical integer — is kept as a string, so a token
     * like an all-digit forwarding secret is never rewritten as a number.
     */
    private fun coerceScalar(value: String): Any = when {
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        else -> value.toLongOrNull()?.takeIf { it.toString() == value } ?: value
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
