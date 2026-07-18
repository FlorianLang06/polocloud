package de.polocloud.node.terminal

import org.jline.reader.Completer

/**
 * The subset of [CliTerminal] an interactive multi-step command (e.g. `group setup`)
 * needs to run a question-by-question wizard. Kept separate from the concrete
 * [CliTerminal] so commands depending on it can be unit-tested with a trivial fake
 * instead of spinning up a real JLine terminal.
 */
interface WizardPrompt {
    fun beginQuiet()
    fun endQuiet()
    fun clearScreen()
    fun display(message: String)
    fun setCompleter(completer: Completer)
    fun resetCompleter()
    fun awaitInput(prompt: String): String
}