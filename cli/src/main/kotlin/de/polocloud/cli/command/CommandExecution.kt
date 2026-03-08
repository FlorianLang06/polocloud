package de.polocloud.cli.command

import de.polocloud.cli.command.arguments.InputContext

/**
 * Represents an action executed when a command or command syntax is matched.
 *
 * Implemented as a functional interface so it can be used with Kotlin lambdas:
 * ```kotlin
 * defaultExecution { ctx -> println("Hello!") }
 * ```
 *
 * @see Command
 * @see CommandSyntax
 */
fun interface CommandExecution {

    /**
     * Executes this command action.
     *
     * @param context The [InputContext] containing all parsed argument values for this invocation.
     */
    fun execute(context: InputContext)
}