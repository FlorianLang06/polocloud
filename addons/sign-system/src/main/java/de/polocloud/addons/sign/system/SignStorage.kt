package de.polocloud.addons.sign.system

import de.polocloud.common.configuration.ConfigurationManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Persists attached [SignEntry]s (minus their live [de.polocloud.shared.service.Service]
 * binding, which is re-resolved on the next [SignSystem.start]) to [file] as JSON.
 *
 * Without this, every attached sign would need to be re-added by hand on every
 * restart — defeating the point of showing already-running servers immediately on
 * boot. Reuses [ConfigurationManager]'s shared `Json` instance for consistency with
 * the rest of the project rather than introducing a second JSON setup.
 */
class SignStorage(private val file: Path) {

    @Serializable
    private data class Entry(
        val type: String,
        val group: String,
        val layoutId: String,
        val position: SignPosition,
    )

    @Serializable
    private data class Document(val entries: List<Entry> = emptyList())

    fun load(): List<SignEntry> {
        if (!file.exists()) return emptyList()

        val document = runCatching {
            ConfigurationManager.json.decodeFromString<Document>(file.readText())
        }.getOrElse { Document() }

        return document.entries.map { entry ->
            SignEntry(SignEntryType(entry.type), entry.position, entry.group, entry.layoutId)
        }
    }

    fun save(entries: Collection<SignEntry>) {
        val document = Document(entries.map { Entry(it.type.id, it.group, it.layoutId, it.position) })

        file.parent?.let(Files::createDirectories)
        file.toFile().writeText(ConfigurationManager.json.encodeToString(document))
    }
}