package dev.httpmarco.polocloud.cli.command.arguments.type

import dev.httpmarco.polocloud.cli.command.arguments.InputContext
import dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument

/**
 * A fixed-keyword argument that only matches when the input equals the argument's [key] exactly.
 *
 * Keyword arguments act as literal tokens in a command syntax, useful for subcommand routing.
 * For example, a syntax like `"group create <name>"` would use a [KeywordArgument] for `"group"`
 * and `"create"` and a dynamic argument for `<name>`.
 *
 * In usage strings, keyword arguments are rendered without angle brackets to distinguish
 * them from dynamic arguments.
 *
 * @param key The expected literal keyword (case-insensitive match).
 */
class KeywordArgument(key: String) : TerminalArgument<String>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf(key)
    }

    override fun predication(rawInput: String): Boolean {
        return super.predication(rawInput) && rawInput.equals(key, ignoreCase = true)
    }

    /**
     * Keywords carry no runtime value — returns an empty string.
     */
    override fun buildResult(input: String, context: InputContext): String = ""

    override fun wrongReason(rawInput: String): String = ""
}