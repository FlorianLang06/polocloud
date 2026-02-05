package dev.httpmarco.polocloud.cli.jline

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

object JLine3Completer : Completer {

    override fun complete(
        reader: LineReader,
        line: ParsedLine,
        candidates: List<Candidate?>
    ) {

    }
}