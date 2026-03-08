package de.polocloud.cli.command.arguments.type

import de.polocloud.cli.command.arguments.InputContext
import de.polocloud.cli.command.arguments.TerminalArgument

/**
 * A greedy argument that captures all remaining input tokens as a single joined string.
 *
 * When [de.polocloud.cli.command.CommandParser] encounters a [StringArrayArgument] as the last argument in a syntax,
 * it joins all remaining tokens (from the current position onward) with spaces and passes
 * the result to [buildResult].
 *
 * This is useful for arguments like messages, descriptions, or any free-form text input
 * that may contain spaces.
 *
 * Example: For input `"say Hello World"`, a `StringArrayArgument` at position 1
 * would receive `"Hello World"` as its input.
 *
 * @param key A short identifier for this argument, shown in help output as `<key>`.
 */
class StringArrayArgument(key: String) : TerminalArgument<String>(key) {

    /**
     * Returns the joined input string as-is.
     */
    override fun buildResult(input: String, context: InputContext): String = input
}