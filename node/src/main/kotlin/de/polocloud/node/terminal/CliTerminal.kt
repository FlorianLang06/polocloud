package de.polocloud.node.terminal

import de.polocloud.common.commands.CommandService
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.terminal.impl.ClearCommand
import de.polocloud.node.terminal.impl.ClusterCommand
import de.polocloud.node.terminal.impl.GroupCommand
import de.polocloud.node.terminal.impl.InfoCommand
import de.polocloud.node.terminal.impl.ServiceCommand
import de.polocloud.node.terminal.impl.ShutdownCommand
import de.polocloud.node.terminal.impl.TemplateCommand
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.nio.charset.StandardCharsets

/**
 * Wraps a JLine 3 terminal and provides a high-level API for displaying output,
 * managing the input prompt, and coordinating command reading.
 *
 * On creation, the terminal connects to the system console with UTF-8 encoding
 * and configures a [LineReaderImpl] with tab-completion, style options, and
 * sensible defaults for a CLI experience.
 *
 * Use [readingThread] to start the background input loop and [shutdown] to gracefully
 * close the terminal and stop the reading thread.
 */
class CliTerminal(val context: NodeRuntimeContext) {

    /**
     * The currently displayed prompt string (ANSI-translated).
     */
    var prompt: String? = AnsiColors.translate("&bpolocloud&8@&7${context.localNodeContainer.data.name()} &8» &7")
    val commandService = CommandService()

    private val terminal = TerminalBuilder.builder()
        .system(true)
        .encoding(StandardCharsets.UTF_8)
        .dumb(true)
        .build()

    private val lineReader: LineReaderImpl = LineReaderBuilder.builder()
        .terminal(this.terminal)
        .completer(CommandCompleter(this.commandService))
        .option(LineReader.Option.AUTO_MENU_LIST, true)
        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        .option(LineReader.Option.AUTO_PARAM_SLASH, false)
        .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
        .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "fg:default")
        .variable(LineReader.BELL_STYLE, "none")
        .build() as LineReaderImpl

    /**
     * The background thread that reads and dispatches user input.
     */
    val readingThread = ReadingThread(this, this.lineReader, this.commandService)

    // Guards every write to the terminal (prompt redraw + log/output printing) so that
    // concurrent callers (the log appender, service log tailing threads, etc.) can't
    // interleave their escape sequences and corrupt the prompt line.
    private val writeLock = Any()

    init {
        this.commandService.registerCommand(
            GroupCommand(
                this.context.groupService,
                this.context.serviceProvider.platformService,
                this.context.serviceProvider,
            )
        )
        this.commandService.registerCommand(
            ServiceCommand(this.context.serviceProvider, this)
        )
        this.commandService.registerCommand(
            ClusterCommand(
                this.context.localNodeContainer,
                this.context.groupService,
                this.context.serviceProvider,
            )
        )
        this.commandService.registerCommand(
            InfoCommand(
                this.context.localNodeContainer,
                this.context.holder,
                this.context.groupService,
                this.context.serviceProvider,
            )
        )
        this.commandService.registerCommand(ShutdownCommand())
        this.commandService.registerCommand(ClearCommand(this))
        this.commandService.registerCommand(TemplateCommand(this.context.groupService))
    }

    /**
     * Clears the entire terminal screen.
     */
    fun clearScreen() = synchronized(writeLock) {
        this.terminal.puts(InfoCmp.Capability.clear_screen)
        this.terminal.flush()
    }

    /**
     * Prints [message] above the current input line. Kept as a separate name for
     * call-site clarity, but routed through the same lock-guarded, JLine-safe path as
     * [displayApproved] so it can never race with it and corrupt the prompt.
     */
    fun display(message: String) = displayApproved(message)

    /**
     * Prints a single blank line above the current input line.
     */
    fun emptyLine() = synchronized(writeLock) {
        this.lineReader.printAbove(" ")
    }

    /**
     * Prints [message] above the current input line without disturbing the prompt.
     */
    fun displayApproved(message: String) = synchronized(writeLock) {
        this.lineReader.printAbove(message)
        this.updateLocked()
    }

    /**
     * Forces the JLine prompt to redraw if the reader is currently active.
     * Called automatically after display operations to keep the UI consistent.
     */
    fun update() = synchronized(writeLock) {
        updateLocked()
    }

    private fun updateLocked() {
        if (this.lineReader.isReading) {
            this.lineReader.callWidget(LineReader.REDRAW_LINE)
            this.lineReader.callWidget(LineReader.REDISPLAY)
        }
    }

    /**
     * Updates the prompt to [prompt] (supports `&x` color codes) and redraws the terminal.
     *
     * @param prompt The new prompt string with optional color codes.
     */
    fun updatePrompt(prompt: String) = synchronized(writeLock) {
        this.prompt = AnsiColors.translate(prompt)
        this.lineReader.setPrompt(this.prompt)
        this.updateLocked()
    }

    /**
     * Clears the current (possibly blank) input line above the prompt in a way that's
     * safe to call concurrently with [display]/[displayApproved].
     */
    fun clearCurrentLine() = synchronized(writeLock) {
        this.terminal.writer().print(
            Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString()
        )
        this.terminal.writer().flush()
    }

    /**
     * Reads a single line of input using the given [prompt].
     *
     * Used by interactive sub-modes (e.g. `service <name> logs` tailing) that need to
     * block for user input while log output is printed above via [displayApproved].
     * Not lock-guarded on purpose: the call blocks until the user submits a line, and
     * holding [writeLock] would stall concurrent output the whole time.
     *
     * @throws org.jline.reader.UserInterruptException on Ctrl+C, which callers may catch
     *         to leave the sub-mode without terminating the node.
     */
    fun awaitInput(prompt: String): String = this.lineReader.readLine(AnsiColors.translate(prompt))

    /**
     * Closes the terminal and interrupts the [readingThread] thread.
     */
    fun shutdown() {
        this.terminal.close()
        this.readingThread.interrupt()
    }
}