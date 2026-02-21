package dev.httpmarco.polocloud.cli.command.arguments.type.string

import dev.httpmarco.polocloud.cli.command.arguments.InputContext
import dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument
import dev.httpmarco.polocloud.i18n.api.TranslationService

/**
 * A terminal argument that accepts any non-blank string as input.
 *
 * This is the simplest dynamic argument type — it performs no type conversion
 * and only rejects blank or empty input.
 *
 * Example:
 * ```kotlin
 * val nameArg = TextArgument("name")
 * syntax({ ctx -> println(ctx.arg(nameArg)) }, nameArg)
 * ```
 *
 * @param key A short identifier for this argument, shown in help output as `<key>`.
 */
class TextArgument(key: String) : TerminalArgument<String>(key) {

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank()
    }

    override fun wrongReason(rawInput: String): String {
        return TranslationService.tr("cli", "cli.argument.string.empty", "key" to key)
    }

    override fun buildResult(input: String, context: InputContext): String = input
}