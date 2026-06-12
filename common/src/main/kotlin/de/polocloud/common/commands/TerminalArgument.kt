package de.polocloud.common.commands

abstract class TerminalArgument<T>(open val key: String) {

    private val shortcuts = arrayListOf<TerminalShortCut>()

    open fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf()
    }

    // if one argument must be a special type
    open fun predication(rawInput: String): Boolean {
        return !(rawInput.startsWith("<") && rawInput.endsWith(">"))
    }

    open fun wrongReason(rawInput: String): String {
        return "node.command.argument.wrong"
    }

    abstract fun buildResult(input: String, context: InputContext): T

    fun bindShortcut(key: Char, value: String) {
        shortcuts.add(TerminalShortCut(key, value))
    }
}