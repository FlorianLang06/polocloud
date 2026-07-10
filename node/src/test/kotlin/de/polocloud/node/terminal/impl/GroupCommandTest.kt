package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.CommandService
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.factory.PlatformService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * In-memory [GroupService] so the command can be exercised without a database.
 * Overrides [update] without super to avoid publishing cluster events in tests.
 */
private class InMemoryGroupService(initial: List<Group>) : GroupService() {
    val storage = initial.associateBy { it.name }.toMutableMap()
    override fun findAll() = storage.values.toList()
    override fun exists(name: String) = storage.containsKey(name)
    override fun find(name: String) = storage[name]
    override fun update(group: Group): Group = group.also { storage[it.name] = it }
}

/**
 * Drives the node `group` command through the real [CommandService]/parser to verify
 * the `list` and `edit` syntaxes wire through to [GroupService].
 */
class GroupCommandTest {

    private lateinit var groups: InMemoryGroupService
    private lateinit var commands: CommandService

    @BeforeEach
    fun setUp() {
        groups = InMemoryGroupService(listOf(Group("lobby", 512, 0.5, 1, 3, "velocity", "3.5.0")))
        commands = CommandService()
        commands.registerCommand(GroupCommand(groups, PlatformService()))
    }

    private fun exec(vararg args: String) =
        commands.parser.findSyntaxCommand(commands.commandsByName("group").single(), arrayOf(*args))

    private fun lobby() = groups.storage.getValue("lobby")

    @Test
    fun `list matches without error`() {
        assertNotNull(exec("list"))
    }

    @Test
    fun `edit memory updates the group`() {
        assertNotNull(exec("edit", "lobby", "memory", "2048"))
        assertEquals(2048, lobby().memory)
    }

    @Test
    fun `edit minOnline and maxOnline update the group`() {
        exec("edit", "lobby", "minOnline", "2")
        exec("edit", "lobby", "maxOnline", "9")
        assertEquals(2, lobby().minOnline)
        assertEquals(9, lobby().maxOnline)
    }

    @Test
    fun `edit startThreshold updates the group`() {
        exec("edit", "lobby", "startThreshold", "0.9")
        assertEquals(0.9, lobby().startThreshold)
    }

    @Test
    fun `edit property sets and persists it as json`() {
        assertNotNull(exec("edit", "lobby", "property", "fallback", "true"))
        assertEquals("true", lobby().properties["fallback"])
        assert(lobby().propertiesJson.contains("fallback"))
    }

    @Test
    fun `edit property supports values with spaces`() {
        exec("edit", "lobby", "property", "motd", "Welcome to the lobby")
        assertEquals("Welcome to the lobby", lobby().properties["motd"])
    }

    @Test
    fun `unset removes a property`() {
        exec("edit", "lobby", "property", "fallback", "true")
        assertNotNull(exec("edit", "lobby", "unset", "fallback"))
        assertFalse(lobby().properties.containsKey("fallback"))
    }

    @Test
    fun `editing an unknown group does not match`() {
        assertNull(exec("edit", "does-not-exist", "memory", "2048"))
    }
}
