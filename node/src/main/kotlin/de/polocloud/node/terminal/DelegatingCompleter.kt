package de.polocloud.node.terminal

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * A [Completer] that forwards to a swappable [delegate].
 *
 * [LineReaderBuilder][org.jline.reader.LineReaderBuilder] only accepts a completer at build
 * time and the built [LineReader] offers no way to replace it afterwards, so interactive
 * sub-modes (e.g. the `group setup` wizard) that need their own per-step completion install
 * their completer here for the duration of the prompt and hand control back to the default
 * one (usually [CommandCompleter]) afterward.
 */
class DelegatingCompleter(@Volatile var delegate: Completer) : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        delegate.complete(reader, line, candidates)
    }
}