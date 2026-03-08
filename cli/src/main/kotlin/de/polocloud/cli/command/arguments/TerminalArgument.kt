package de.polocloud.cli.command.arguments

/**
 * Base class for all command argument types used in [dev.httpmarco.polocloud.cli.command.CommandSyntax] definitions.
 *
 * An argument handles three responsibilities:
 * - **Predication**: deciding whether a raw input string is valid for this argument type.
 * - **Building**: converting the raw input string into a typed result [T].
 * - **Completion**: providing default suggestions for tab-completion via [defaultArgs].
 *
 * Subclasses override [predication] to restrict which inputs are accepted and
 * [buildResult] to parse the input into the appropriate type.
 *
 * @param T The result type produced by this argument after parsing.
 * @param key A short identifier for this argument, used in help output and [InputContext] lookup.
 */
abstract class TerminalArgument<T>(open val key: String) {

    private val shortcuts = arrayListOf<TerminalShortCut>()

    /**
     * Returns a list of default tab-completion suggestions for this argument.
     *
     * @param context The current [InputContext] with previously parsed arguments.
     * @return Suggested completion strings for this argument position.
     */
    open fun defaultArgs(context: InputContext): MutableList<String> = mutableListOf()


    /**
     * Returns `true` if [rawInput] is a valid value for this argument.
     *
     * The default implementation rejects strings wrapped in angle brackets (e.g. `<value>`),
     * which are typically placeholder literals rather than real input.
     *
     * @param rawInput The raw string token from the terminal input.
     */
    open fun predication(rawInput: String): Boolean {
        return !(rawInput.startsWith("<") && rawInput.endsWith(">"))
    }

    /**
     * Returns a human-readable explanation of why [rawInput] failed predication.
     *
     * @param rawInput The invalid input that was rejected.
     * @return A user-facing error message. Empty string if no specific reason is available.
     */
    open fun wrongReason(rawInput: String): String = "" // TODO: i18n

    /**
     * Converts [input] into the typed result [T] for this argument.
     *
     * @param input The validated raw input string.
     * @param context The current [InputContext] with all previously parsed argument values.
     * @return The parsed result of type [T].
     */
    abstract fun buildResult(input: String, context: InputContext): T

    /**
     * Registers a shortcut character [key] that expands to [value] during input.
     *
     * @param key The single character shortcut key.
     * @param value The full string this shortcut expands to.
     */
    fun bindShortcut(key: Char, value: String) {
        shortcuts.add(TerminalShortCut(key, value))
    }
}