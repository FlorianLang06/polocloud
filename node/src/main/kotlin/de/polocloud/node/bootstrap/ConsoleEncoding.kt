package de.polocloud.node.bootstrap

import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Ensures the process console can render the UTF-8 output produced by the
 * loggers and the JLine terminal (arrows, box drawing characters, emoji, ...).
 *
 * On Windows the console defaults to a legacy OEM code page (e.g. CP850), so the
 * UTF-8 bytes written by log4j are rendered as mojibake such as `Ôåô` instead of
 * `↓`. Switching the active code page to 65001 (UTF-8) fixes the rendering for
 * the whole console — the child `chcp` process shares the parent console, so the
 * change applies to this JVM's output as well.
 *
 * `System.out` / `System.err` are additionally rebound to UTF-8 [PrintStream]s so
 * that direct `print`/`println` calls encode their text consistently.
 */
fun enableUtf8Console() {
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8))

    if (!System.getProperty("os.name").lowercase().contains("win")) return

    // `chcp` changes the code page of the shared parent console regardless of
    // where its own stdio is routed, so discard its "Active code page" message.
    runCatching {
        ProcessBuilder("cmd", "/c", "chcp", "65001")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor()
    }
}