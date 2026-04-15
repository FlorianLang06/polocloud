package de.polocloud.cli.command

import de.polocloud.cli.command.impl.ClearCommand
import de.polocloud.cli.command.impl.cluster.ConnectCommand
import de.polocloud.cli.command.impl.HelpCommand
import de.polocloud.cli.command.impl.ShutdownCommand
import de.polocloud.cli.command.impl.cluster.NodesCommand
import de.polocloud.cli.command.impl.cluster.ServicesCommand
import de.polocloud.cli.communication.connection.CliConnectionManager

/**
 * Central registry and dispatcher for all CLI commands.
 *
 * Commands are registered on startup and looked up by name or alias on each input.
 * Parsing and execution is delegated to [CommandParser].
 *
 * Built-in commands are registered automatically in [init].
 */
class CommandService(
    connectionManager: CliConnectionManager
) {

    private val registeredCommands = mutableListOf<Command>()
    private val parser = CommandParser(this)

    init {
        registerCommand(HelpCommand())
        registerCommand(ShutdownCommand())
        registerCommand(ClearCommand())
        registerCommand(ConnectCommand(connectionManager))
        registerCommand(NodesCommand(connectionManager))
        registerCommand(ServicesCommand(connectionManager))
    }

    /**
     * Returns all commands that match the given [name] (case-insensitive),
     * including commands registered under that name as an alias.
     *
     * @param name The command name or alias to look up.
     * @return A list of matching commands (usually zero or one).
     */
    fun findByName(name: String): List<Command> {
        return registeredCommands.filter { command ->
            command.name.equals(name, ignoreCase = true) ||
                    command.aliases.any { it.equals(name, ignoreCase = true) }
        }
    }

    /**
     * Registers a single [command] in the command registry.
     *
     * @param command The command to register.
     */
    fun registerCommand(command: Command) {
        registeredCommands.add(command)
    }

    /**
     * Registers multiple [commands] at once.
     *
     * @param commands The commands to register.
     */
    fun registerCommands(vararg commands: Command) {
        commands.forEach(::registerCommand)
    }

    /**
     * Removes a [command] from the registry.
     *
     * @param command The command to unregister.
     */
    fun unregisterCommand(command: Command) {
        registeredCommands.remove(command)
    }

    /**
     * Parses and dispatches the command identified by [commandId] with the given [args].
     *
     * @param commandId The command name or alias entered by the user.
     * @param args The arguments following the command name.
     */
    fun call(commandId: String, args: Array<String>) {
        parser.parse(commandId, args)
    }

    /**
     * @return list of registered Commands.
     */
    fun registeredCommands() = this.registeredCommands
}