package de.polocloud.cli.command.impl

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.CommandService
import de.polocloud.cli.command.CommandSyntax
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService

class HelpCommand : Command("help", "cli.command.impl.help.description", "h", "?") {

    init {
        defaultExecution {
            logger.info(TranslationService.tr("cli", "cli.command.impl.help.header"))

            Cli.terminal.commandService.registeredCommands().sortedBy { it.name }.forEach { command ->
                val aliases = command.aliases.joinToString(", ")

                if (aliases.isNotEmpty()) {
                    logger.info(" &8- &f${command.name} &7(&f$aliases&7)&8: &7${command.description}")
                } else {
                    logger.info(" &8- &f${command.name}&8: &7${command.description}")
                }
            }
        }
    }
}
