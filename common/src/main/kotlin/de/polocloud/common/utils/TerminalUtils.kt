package de.polocloud.common.utils

object TerminalUtils {

    private const val ESC = "\u001B["

    fun clear() {
        print("\u001B[H\u001B[2J")
        System.out.flush()
    }

    fun reset() {
        print("${ESC}0m")
    }
}