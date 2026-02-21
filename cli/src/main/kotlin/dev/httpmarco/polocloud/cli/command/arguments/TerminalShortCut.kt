package dev.httpmarco.polocloud.cli.command.arguments

/**
 * Represents a single-character shortcut that expands to a full string value during command input.
 *
 * Shortcuts can be bound to a [TerminalArgument] via [TerminalArgument.bindShortcut]
 * and are intended to speed up repetitive input patterns.
 *
 * @param key The shortcut trigger character (e.g. `'s'` for `"start"`).
 * @param value The full string this shortcut expands to.
 */
data class TerminalShortCut(val key: Char, val value: String)