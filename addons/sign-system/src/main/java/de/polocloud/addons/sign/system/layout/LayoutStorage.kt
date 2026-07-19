package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignEntryType
import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.configuration.watcher.FileWatcher
import de.polocloud.shared.service.ServiceState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Persists [SignLayout] definitions — frames, colors, animation timing, banner pattern
 * design, hologram positioning — to [file] as JSON, the way [de.polocloud.addons.sign.system.SignStorage]
 * persists attached entries. Without this, an operator could never change a frame's text,
 * colors or timing without recompiling the addon: [LayoutRegistry] previously only ever
 * held the hardcoded [LayoutRegistry.defaultSignLayout].
 *
 * Reuses [ConfigurationManager]'s shared `Json` instance, same reasoning as [de.polocloud.addons.sign.system.SignStorage]:
 * the file lives under the plugin's runtime-resolved data folder, which the static-path
 * [de.polocloud.common.configuration.ConfigurationFile] annotation can't express.
 */
class LayoutStorage(private val file: Path) {

    @Serializable
    private data class LayoutDto(
        val id: String,
        val type: String,
        val animations: Map<ServiceState, StateAnimation> = emptyMap(),
    )

    @Serializable
    private data class Document(val layouts: List<LayoutDto> = emptyList())

    private var watcher: FileWatcher? = null

    /** Loads persisted layouts, or seeds [file] with [seed]'s result if it doesn't exist yet (or fails to parse). */
    fun load(seed: () -> List<SignLayout>): List<SignLayout> {
        if (!file.exists()) {
            val defaults = seed()
            save(defaults)
            return defaults
        }

        val document = runCatching {
            ConfigurationManager.json.decodeFromString<Document>(file.readText())
        }.getOrNull()

        if (document == null || document.layouts.isEmpty()) return seed()

        return document.layouts.map { dto ->
            SignLayout(dto.id, SignEntryType(dto.type)).also { layout ->
                dto.animations.forEach { (state, animation) -> layout.set(state, animation) }
            }
        }
    }

    fun save(layouts: Collection<SignLayout>) {
        val document = Document(layouts.map { LayoutDto(it.id, it.type.id, it.explicitAnimations()) })

        file.parent?.let(Files::createDirectories)
        file.toFile().writeText(ConfigurationManager.json.encodeToString(document))
    }

    /** Calls [onReload] whenever [file] is modified on disk, e.g. by an operator hand-editing it. */
    fun watch(onReload: () -> Unit) {
        watcher = FileWatcher(file, onReload)
    }

    fun stopWatching() = watcher?.stop()
}
