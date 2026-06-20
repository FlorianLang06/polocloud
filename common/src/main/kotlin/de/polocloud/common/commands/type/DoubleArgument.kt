package de.polocloud.common.commands.type

open class DoubleArgument(
    key: String,
    minValue: Double? = null,
    maxValue: Double? = null,
    defaultValue: Double? = null
) : NumberArgument<Double>(key, minValue, maxValue, defaultValue) {

    override val translationKey = "node.command.double"

    override fun parse(rawInput: String): Double = rawInput.toDouble()
}
