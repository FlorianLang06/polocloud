package de.polocloud.node.terminal

import de.polocloud.common.commands.CommandService
import de.polocloud.common.commands.InputContext
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * JLine [Completer] that derives its suggestions from the registered command syntaxes.
 *
 * Completion walks the already-typed words and, for the argument currently being typed,
 * asks every matching [de.polocloud.common.commands.CommandSyntax] for its
 * [de.polocloud.common.commands.TerminalArgument.defaultArgs]. Preceding words are parsed
 * into an [InputContext] beforehand, so context-aware arguments (e.g. a platform version
 * that depends on the chosen platform) can offer the correct, dependent suggestions.
 */
class CommandCompleter(private val commandService: CommandService) : Completer {

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val words = line.words()
        val wordIndex = line.wordIndex()

        // completing the command name itself
        if (wordIndex == 0) {
            commandService.commands.forEach { candidates.add(Candidate(it.name)) }
            return
        }

        val command = commandService.commandsByName(words[0]).firstOrNull() ?: return
        val argIndex = wordIndex - 1

        for (syntax in command.commandSyntaxes) {
            if (argIndex >= syntax.arguments.size) continue

            val context = InputContext()
            if (!fillContext(syntax.arguments, words, argIndex, context)) continue

            syntax.arguments[argIndex].defaultArgs(context).forEach { candidates.add(Candidate(it)) }
        }
    }

    /**
     * Parses the words preceding the one being completed into [context].
     * Returns `false` as soon as a word does not satisfy its argument, marking the syntax
     * as not applicable for the current input.
     */
    private fun fillContext(
        arguments: List<de.polocloud.common.commands.TerminalArgument<*>>,
        words: List<String>,
        argIndex: Int,
        context: InputContext
    ): Boolean {
        for (i in 0 until argIndex) {
            val argument = arguments[i]
            val raw = words[i + 1]
            if (!argument.predication(raw)) return false
            val appended = runCatching { context.append(argument, argument.buildResult(raw, context)) }.isSuccess
            if (!appended) return false
        }
        return true
    }
}
