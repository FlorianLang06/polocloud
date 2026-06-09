package de.polocloud.service.factory.process

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Downloads and manages local JRE installations for specific Java major versions.
 *
 * Runtimes are sourced from the [Adoptium API](https://adoptium.net) (Eclipse Temurin)
 * and cached under `runtimes/java-{version}/`. The OS and CPU architecture are
 * detected automatically at runtime.
 */
object JavaRuntimeManager {

    private val runtimesDir = File("runtimes")

    /**
     * Ensures the JRE for [javaVersion] is available locally.
     * Downloads and extracts it from Adoptium if not yet cached.
     *
     * @param javaVersion Java major version required (e.g. 8, 17, 21).
     * @return The java executable [File] for the requested version.
     * @throws IllegalStateException if the executable cannot be located after extraction.
     */
    @Synchronized
    fun ensure(javaVersion: Int): File {
        val runtimeDir = File(runtimesDir, "java-$javaVersion")
        findExecutable(runtimeDir)?.let { return it }

        println("  ☕ Java $javaVersion not found — fetching from Adoptium ...")
        val (downloadUrl, fileName) = fetchDownloadInfo(javaVersion)
        val archive = File(runtimeDir.also { it.mkdirs() }, fileName)

        print("  ↓ Downloading $fileName ...")
        System.out.flush()
        URI(downloadUrl).toURL().openStream().use { input ->
            archive.outputStream().use { input.copyTo(it) }
        }
        println("  done (${archive.length() / 1024 / 1024} MB)")

        extractArchive(archive, runtimeDir)
        archive.delete()

        return findExecutable(runtimeDir)
            ?: error("Java $javaVersion executable not found after extraction in ${runtimeDir.absolutePath}")
    }

    /**
     * Queries the Adoptium v3 API for the latest JRE of [javaVersion] matching
     * the current OS and architecture.
     *
     * @return Pair of (download URL, file name).
     */
    private fun fetchDownloadInfo(javaVersion: Int): Pair<String, String> {
        val url = "https://api.adoptium.net/v3/assets/latest/$javaVersion/hotspot" +
                "?os=${detectOs()}&architecture=${detectArch()}&image_type=jre"
        val root = Json.parseToJsonElement(URI(url).toURL().readText()).jsonArray
        val pkg = root[0].jsonObject["binary"]!!.jsonObject["package"]!!.jsonObject
        return pkg["link"]!!.jsonPrimitive.content to pkg["name"]!!.jsonPrimitive.content
    }

    /**
     * Searches [dir] recursively for the java executable inside any `bin/` subdirectory.
     * Sets the executable bit on non-Windows systems.
     *
     * @return The executable [File], or null if [dir] does not exist or contains no match.
     */
    private fun findExecutable(dir: File): File? {
        if (!dir.exists()) return null
        val execName = if (isWindows()) "java.exe" else "java"
        return dir.walkTopDown()
            .firstOrNull { it.name == execName && it.parentFile?.name == "bin" }
            ?.also { if (!isWindows()) it.setExecutable(true) }
    }

    private fun extractArchive(archive: File, targetDir: File) {
        if (archive.name.endsWith(".zip")) extractZip(archive, targetDir)
        else extractTarGz(archive, targetDir)
    }

    /**
     * Extracts a ZIP archive into [targetDir].
     * The top-level directory included in Adoptium ZIPs is stripped automatically.
     */
    private fun extractZip(archive: File, targetDir: File) {
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val relative = entry.name.substringAfter("/")
                if (relative.isNotBlank()) {
                    val out = File(targetDir, relative)
                    if (entry.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /**
     * Extracts a `.tar.gz` archive via the system `tar` command.
     * `--strip-components=1` removes the root JDK directory prefix.
     */
    private fun extractTarGz(archive: File, targetDir: File) {
        ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", targetDir.absolutePath, "--strip-components=1")
            .inheritIO()
            .start()
            .waitFor()
    }

    private fun detectOs(): String = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("win") -> "windows"
            it.contains("mac") -> "mac"
            else -> "linux"
        }
    }

    private fun detectArch(): String = when (System.getProperty("os.arch").lowercase()) {
        "aarch64" -> "aarch64"
        else -> "x64"
    }

    private fun isWindows(): Boolean = detectOs() == "windows"
}
