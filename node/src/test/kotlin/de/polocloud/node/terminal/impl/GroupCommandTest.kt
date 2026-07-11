package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.CommandService
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.factory.PlatformService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
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
    override fun delete(group: Group) { storage.remove(group.name) }
}

/**
 * Drives the node `group` command through the real [CommandService]/parser to verify
 * the `list` and `edit` syntaxes wire through to [GroupService].
 */
class GroupCommandTest {

    private lateinit var groups: InMemoryGroupService
    private lateinit var commands: CommandService

    companion object {
        // The create/delete syntaxes log through the i18n helpers, which require the
        // TranslationService to be initialised once before any command runs.
        @JvmStatic
        @BeforeAll
        fun initTranslations() {
            runCatching { TranslationService.init() }
        }
    }

    @BeforeEach
    fun setUp() {
        groups = InMemoryGroupService(listOf(Group("lobby", 512, 0.5, 1, 3, "velocity", "3.5.0")))
        commands = CommandService()
        commands.registerCommand(GroupCommand(groups, PlatformService(), ServiceProvider()))
    }

    @AfterEach
    fun cleanupTemplateFolders() {
        GroupTemplateService.directoryOf("custom").deleteRecursively()
    }

    private fun exec(vararg args: String) =
        commands.parser.findSyntaxCommand(commands.commandsByName("group").single(), arrayOf(*args))

    private fun lobby() = groups.storage.getValue("lobby")

    @Test
    fun `list matches without error`() {
        assertNotNull(exec("list"))
    }

    @Test
    fun `info matches for an existing group`() {
        assertNotNull(exec("info", "lobby"))
    }

    @Test
    fun `info does not match for an unknown group`() {
        assertNull(exec("info", "does-not-exist"))
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
    fun `edit template add appends a template and ensures its folder`() {
        assertNotNull(exec("edit", "lobby", "template", "add", "custom"))
        assertEquals(listOf("custom"), lobby().templates)
        assert(GroupTemplateService.directoryOf("custom").isDirectory)
    }

    @Test
    fun `edit template add is a no-op for an already-present template`() {
        exec("edit", "lobby", "template", "add", "custom")
        exec("edit", "lobby", "template", "add", "custom")
        assertEquals(listOf("custom"), lobby().templates)
    }

    @Test
    fun `edit template remove removes a template`() {
        exec("edit", "lobby", "template", "add", "custom")
        assertNotNull(exec("edit", "lobby", "template", "remove", "custom"))
        assertFalse(lobby().templates.contains("custom"))
    }

    @Test
    fun `edit template remove is a no-op for a template that is not present`() {
        exec("edit", "lobby", "template", "remove", "custom")
        assertFalse(lobby().templates.contains("custom"))
    }

    @Test
    fun `editing an unknown group does not match`() {
        assertNull(exec("edit", "does-not-exist", "memory", "2048"))
    }

    @Test
    fun `delete removes the group`() {
        assertNotNull(exec("delete", "lobby"))
        assertFalse(groups.storage.containsKey("lobby"))
    }
}
