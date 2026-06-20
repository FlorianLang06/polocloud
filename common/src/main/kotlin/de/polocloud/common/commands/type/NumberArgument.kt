package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

abstract class NumberArgument<T : Comparable<T>>(
    key: String,
    val minValue: T? = null,
    val maxValue: T? = null,
    val defaultValue: T? = null
) : TerminalArgument<T>(key) {

    // the translation prefix used for error messages, e.g. "node.command.int"
    protected abstract val translationKey: String

    // parses the raw input into the concrete number type or throws NumberFormatException
    protected abstract fun parse(rawInput: String): T

    override fun predication(rawInput: String): Boolean {
        if (rawInput.isBlank() && defaultValue != null) return true
        return getError(rawInput) == null
    }

    override fun wrongReason(rawInput: String): String {
        return when (getError(rawInput)) {
            NumberArgumentError.INVALID_NUMBER -> "$translationKey.invalid"
            NumberArgumentError.LOWER_THAN_MIN -> "$translationKey.lower_than_min"
            NumberArgumentError.HIGHER_THAN_MAX -> "$translationKey.higher_than_max"
            else -> ""
        }
    }

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf()
    }

    override fun buildResult(input: String, context: InputContext): T {
        if (input.isBlank() && defaultValue != null) {
            return defaultValue
        }
        return parse(input)
    }

    private fun getError(rawInput: String): NumberArgumentError? {
        try {
            val value = parse(rawInput)

            if (maxValue != null && value > maxValue) {
                return NumberArgumentError.HIGHER_THAN_MAX
            }

            if (minValue != null && value < minValue) {
                return NumberArgumentError.LOWER_THAN_MIN
            }

            return null
        } catch (_: NumberFormatException) {
            return NumberArgumentError.INVALID_NUMBER
        }
    }
}

enum class NumberArgumentError {
    INVALID_NUMBER,
    LOWER_THAN_MIN,
    HIGHER_THAN_MAX,
}
