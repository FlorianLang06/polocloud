package de.polocloud.cli.terminal

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * Provides tab-completion suggestions for the CLI terminal input.
 *
 * Registered as a singleton completer in [CliTerminal] via JLine's [org.jline.reader.LineReaderBuilder].
 * Completion logic will be populated once command registration supports dynamic argument hints.
 */
object TabCompleter : Completer {

    override fun complete(
        reader: LineReader,
        line: ParsedLine,
        candidates: List<Candidate?>
    ) {

    }
}