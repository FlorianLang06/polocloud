package de.polocloud.common.commands.type

open class LongArgument(
    key: String,
    minValue: Long? = null,
    maxValue: Long? = null,
    defaultValue: Long? = null
) : NumberArgument<Long>(key, minValue, maxValue, defaultValue) {

    override val translationKey = "node.command.long"

    override fun parse(rawInput: String): Long = rawInput.toLong()
}
