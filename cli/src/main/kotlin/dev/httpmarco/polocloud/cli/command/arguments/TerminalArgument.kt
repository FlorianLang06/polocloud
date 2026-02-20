package dev.httpmarco.polocloud.cli.command.arguments

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
        return ""//i18n.get("agent.terminal.command.reason.wrong") //TODO
    }

    abstract fun buildResult(input: String, context: InputContext): T

    fun bindShortcut(key: Char, value: String) {
        shortcuts.add(TerminalShortCut(key, value))
    }
}