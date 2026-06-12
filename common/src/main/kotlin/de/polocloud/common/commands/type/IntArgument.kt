package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

open class IntArgument(
    key: String,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val defaultValue: Int? = null
) : TerminalArgument<Int>(key) {
    override fun predication(rawInput: String): Boolean {
        if (rawInput.isBlank() && defaultValue != null) return true
        return getError(rawInput) == null
    }

    override fun wrongReason(rawInput: String): String {
        return when (getError(rawInput)) {
            IntArgumentError.INVALID_INT -> "node.command.int.invalid"
            IntArgumentError.LOWER_THAN_MIN -> "node.command.int.lower_than_min"
            IntArgumentError.HIGHER_THAN_MAX -> "node.command.int.higher_than_max"
            else -> ""
        }
    }

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf()
    }

    override fun buildResult(input: String, context: InputContext): Int {
        if (input.isBlank() && defaultValue != null) {
            return defaultValue
        }
        return input.toInt()
    }

    private fun getError(rawInput: String): IntArgumentError? {
        try {
            val intValue = rawInput.toInt()

            if (maxValue != null && intValue > maxValue) {
                return IntArgumentError.HIGHER_THAN_MAX
            }

            if (minValue != null && intValue < minValue) {
                return IntArgumentError.LOWER_THAN_MIN
            }

            return null
        } catch (_: NumberFormatException) {
            return IntArgumentError.INVALID_INT
        }
    }
}

enum class IntArgumentError {
    INVALID_INT,
    LOWER_THAN_MIN,
    HIGHER_THAN_MAX,
}