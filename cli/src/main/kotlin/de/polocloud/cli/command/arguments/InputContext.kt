package de.polocloud.cli.command.arguments

/**
 * Holds all parsed argument values for a single command invocation.
 *
 * Arguments are stored by their [TerminalArgument.key] and retrieved in a type-safe manner
 * via [arg]. The context is built up incrementally by [dev.httpmarco.polocloud.cli.command.CommandParser] as each argument is matched.
 *
 * Example:
 * ```kotlin
 * val name: String = context.arg(nameArgument)
 * ```
 */
class InputContext {
    private val values = HashMap<String, Any?>()

    /**
     * Retrieves the parsed value for the given [argument].
     *
     * @param argument The argument whose value should be returned.
     * @return The parsed value cast to type [T].
     * @throws ClassCastException if the stored value does not match the expected type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> arg(argument: TerminalArgument<T>): T {
        return values[argument.key] as T
    }

    /**
     * Returns `true` if a value has been stored for the given [argument].
     *
     * @param argument The argument to check.
     */
    fun contains(argument: TerminalArgument<*>): Boolean {
        return values.containsKey(argument.key)
    }

    /**
     * Stores a parsed [value] for the given [argument].
     *
     * @param argument The argument whose value is being stored.
     * @param value The parsed value to associate with this argument.
     */
    fun append(argument: TerminalArgument<*>, value: Any?) {
        values[argument.key] = value
    }

    /**
     * Removes the stored value for the given [argument].
     *
     * @param argument The argument whose value should be removed.
     */
    fun remove(argument: TerminalArgument<*>) {
        values.remove(argument.key)
    }
}