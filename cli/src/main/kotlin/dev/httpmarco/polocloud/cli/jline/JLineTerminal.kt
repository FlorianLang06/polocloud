package dev.httpmarco.polocloud.cli.jline;

import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.nio.charset.StandardCharsets


class JLine3Terminal {

    var prompt: String? = null

    private val terminal = TerminalBuilder.builder()
        .system(true)
        .encoding(StandardCharsets.UTF_8)
        .dumb(false)
        .jansi(true)
        .build()

    private val lineReader: LineReaderImpl = LineReaderBuilder.builder()
        .terminal(this.terminal)
        .completer(JLine3Completer)
        .option(LineReader.Option.AUTO_MENU_LIST, true)
        .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
        .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "fg:default")
        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        .option(LineReader.Option.AUTO_PARAM_SLASH, false)
        .variable(LineReader.BELL_STYLE, "none")
        .build() as LineReaderImpl

    fun clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    fun display(message: String) {
        this.terminal.puts(InfoCmp.Capability.carriage_return)
        this.terminal.writer().println(message)
        this.terminal.flush()
        this.update()
    }

    fun emptyLine() {
        this.lineReader.printAbove(" ")
    }

    fun displayApproved(message: String) {
        this.lineReader.printAbove(message)
        this.update()
    }

    fun update() {
        if (this.lineReader.isReading) {
            this.lineReader.callWidget(LineReader.REDRAW_LINE)
            this.lineReader.callWidget(LineReader.REDISPLAY)
        }
    }

    fun updatePrompt(prompt: String) {
        this.prompt = JLine3Colors.translate(prompt)
        this.lineReader.setPrompt(this.prompt)
        this.update()
    }

    fun resetPrompt() {
        //  this.updatePrompt("&bpolocloud&8@&7" + polocloudVersion() + " &8» &7")
    }

    fun shutdown() {
        try {
            terminal.writer().println()
            terminal.flush()
        } finally {
            terminal.close()
        }
    }
}