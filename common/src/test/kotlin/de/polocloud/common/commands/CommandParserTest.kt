package de.polocloud.common.commands

import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.common.commands.type.TextArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A command exposing a keyword-only syntax and a greedy `<name> <value...>` syntax,
 * capturing which syntax fired so the parser can be asserted end-to-end.
 */
private class SpyCommand : Command("test", "desc", "t") {

    var executed: String? = null

    val nameArgument = TextArgument("name")
    val valueArgument = StringArrayArgument("value")

    init {
        defaultExecution { executed = "default" }
        syntax({ executed = "list" }, KeywordArgument("list"))
        syntax({ ctx -> executed = "edit:${ctx.arg(nameArgument)}:${ctx.arg(valueArgument)}" },
            KeywordArgument("edit"), nameArgument, valueArgument)
    }
}

class CommandParserTest {

    private fun setup(): Pair<CommandService, SpyCommand> {
        val service = CommandService()
        val command = SpyCommand()
        service.registerCommand(command)
        return service to command
    }

    @Test
    fun `keyword-only syntax matches`() {
        val (service, command) = setup()
        val syntax = service.parser.findSyntaxCommand(command, arrayOf("list"))
        assertNotNull(syntax)
        assertEquals("list", command.executed)
    }

    @Test
    fun `keyword matching is case-insensitive`() {
        val (service, command) = setup()
        service.parser.findSyntaxCommand(command, arrayOf("LIST"))
        assertEquals("list", command.executed)
    }

    @Test
    fun `greedy last argument joins the remaining tokens`() {
        val (service, command) = setup()
        val syntax = service.parser.findSyntaxCommand(command, arrayOf("edit", "lobby", "hello", "world"))
        assertNotNull(syntax)
        assertEquals("edit:lobby:hello world", command.executed)
    }

    @Test
    fun `greedy syntax with fewer tokens than its fixed prefix does not throw and does not match`() {
        val (service, command) = setup()
        // Regression: previously indexed args[i] unguarded and threw
        // ArrayIndexOutOfBoundsException for "edit" alone.
        val syntax = service.parser.findSyntaxCommand(command, arrayOf("edit"))
        assertNull(syntax)
        assertNull(command.executed)
    }

    @Test
    fun `greedy syntax missing its trailing value does not match`() {
        val (service, command) = setup()
        val syntax = service.parser.findSyntaxCommand(command, arrayOf("edit", "lobby"))
        assertNull(syntax)
        assertNull(command.executed)
    }

    @Test
    fun `unknown first token matches no syntax`() {
        val (service, command) = setup()
        assertNull(service.parser.findSyntaxCommand(command, arrayOf("nope")))
        assertNull(command.executed)
    }

    @Test
    fun `no-argument call runs the default execution`() {
        val (service, command) = setup()
        service.call("test", emptyArray())
        assertEquals("default", command.executed)
    }

    @Test
    fun `commandsByName resolves name and alias`() {
        val (service, command) = setup()
        assertEquals(command, service.commandsByName("test").single())
        assertEquals(command, service.commandsByName("T").single())
        assertEquals(0, service.commandsByName("missing").size)
    }
}
