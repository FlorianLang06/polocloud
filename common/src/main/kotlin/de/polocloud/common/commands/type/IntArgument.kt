package de.polocloud.common.commands.type

open class IntArgument(
    key: String,
    minValue: Int? = null,
    maxValue: Int? = null,
    defaultValue: Int? = null
) : NumberArgument<Int>(key, minValue, maxValue, defaultValue) {

    override val translationKey = "node.command.int"

    override fun parse(rawInput: String): Int = rawInput.toInt()
}
