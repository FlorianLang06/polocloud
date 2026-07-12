package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CliTerminal
import de.polocloud.node.terminal.types.ServiceArgument
import de.polocloud.node.terminal.types.TemplateArgument
import org.jline.reader.UserInterruptException
import org.slf4j.LoggerFactory

/**
 * Terminal command for inspecting and controlling the services running on this node.
 *
 * Runs in-process, so it talks to the [ServiceProvider] directly (no gRPC): `list`,
 * `<name>` (info), `<name> shutdown`, `<name> logs` (live tail), `<name> execute <command>`
 * and `<name> copy <templateName>` (re-apply a template onto the running work directory).
 */
class ServiceCommand(
    private val serviceProvider: ServiceProvider,
    private val terminal: CliTerminal,
) : Command("service", "Manage all your cloud services", "ser") {

    private val logger = LoggerFactory.getLogger(ServiceCommand::class.java)

    private val serviceArgument = ServiceArgument("name", serviceProvider)
    private val commandArgument = StringArrayArgument("command")
    private val templateArgument = TemplateArgument("templateName")

    init {
        syntax({
            val services = serviceProvider.findAll()
            if (services.isEmpty()) {
                logger.info("There are no services.")
                return@syntax
            }
            logger.info("Services (${services.size}):")
            services.forEach { service ->
                logger.info("  ${service.name()} | state: ${service.state} | port: ${service.port}")
            }
        }, "List all services", KeywordArgument("list"))

        syntax({ context ->
            info(context.arg(serviceArgument))
        }, "Show detailed information about a service", serviceArgument)

        syntax({ context ->
            shutdown(context.arg(serviceArgument))
        }, "Shutdown a service", serviceArgument, KeywordArgument("shutdown"))

        syntax({ context ->
            tailLogs(context.arg(serviceArgument))
        }, "Live-tail the console of a service", serviceArgument, KeywordArgument("logs"))

        syntax({ context ->
            execute(context.arg(serviceArgument), context.arg(commandArgument))
        }, "Execute a command in a service's console", serviceArgument, KeywordArgument("execute"), commandArgument)

        syntax({ context ->
            copy(context.arg(serviceArgument), context.arg(templateArgument))
        }, "Copy a template into a service's work directory", serviceArgument, KeywordArgument("copy"), templateArgument)
    }

    private fun info(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        logger.info("Service ${service.name()}:")
        logger.info("  id: ${service.id}")
        logger.info("  group: ${service.groupName}")
        logger.info("  state: ${service.state}")
        logger.info("  host: ${service.hostname}:${service.port}")
        logger.info("  pid: ${local?.process?.pid() ?: "-"}")
        // Only a co-located LocalService carries a live ping result; a service known only
        // from the DB (e.g. running on another node) has no player count to report here.
        logger.info("  players: ${local?.let { "${it.onlinePlayers}/${it.maxPlayers}" } ?: "-"}")
        logger.info("  motd: ${local?.motd?.takeIf { it.isNotEmpty() } ?: "-"}")
        // Templates actually copied into this instance's work directory on start — not
        // re-read from the group, so it stays accurate even if the group's list changed
        // (or the service isn't running here at all) since this service started.
        logger.info("  templates: ${local?.templates?.takeIf { it.isNotEmpty() }?.joinToString() ?: "-"}")
        val properties = local?.properties.orEmpty()
        if (properties.isEmpty()) {
            logger.info("  properties: (none)")
        } else {
            logger.info("  properties:")
            properties.forEach { (key, value) -> logger.info("    - $key=$value") }
        }
    }

    private fun shutdown(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        if (local == null) {
            logger.info("Service ${service.name()} is not running on this node.")
            return
        }
        logger.info("Shutting down ${service.name()} ...")
        serviceProvider.shutdownLocal(local)
        logger.info("Service ${service.name()} was stopped.")
    }

    private fun execute(service: Service, command: String) {
        val local = serviceProvider.findLocal(service.name())
        if (local == null) {
            logger.info("Service ${service.name()} is not running on this node.")
            return
        }
        if (local.executeCommand(command)) {
            logger.info("Executed '$command' in ${service.name()}.")
        } else {
            logger.info("Could not send the command to ${service.name()} (process not running).")
        }
    }

    private fun copy(service: Service, templateName: String) {
        val local = serviceProvider.findLocal(service.name())
        if (local == null) {
            logger.info("Service ${service.name()} is not running on this node.")
            return
        }
        val workDir = local.workDir
        if (workDir == null) {
            logger.info("Service ${service.name()} has no work directory yet.")
            return
        }
        GroupTemplateService.copyInto(listOf(templateName), workDir.toFile())
        local.templates = local.templates + templateName
        logger.info("Copied template '$templateName' into ${service.name()}.")
    }

    private fun tailLogs(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        if (local == null) {
            logger.info("Service ${service.name()} is not running on this node.")
            return
        }

        logger.info("Tailing logs of ${service.name()} — press Ctrl+C or type 'exit' to stop.")
        // Print the buffered history first, then follow live lines above the input prompt.
        local.recentLogs().forEach { terminal.display(it) }

        val listener: (String) -> Unit = { line -> terminal.display(line) }
        local.addLogListener(listener)
        try {
            while (true) {
                val input = terminal.awaitInput("&8[logs:${service.name()}]&r ").trim()
                if (input.equals("exit", ignoreCase = true)) break
            }
        } catch (_: UserInterruptException) {
            // Ctrl+C leaves the tail without terminating the node.
        } finally {
            local.removeLogListener(listener)
            logger.info("Stopped tailing ${service.name()}.")
        }
    }
}
