package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.services.ServiceProvider
import org.slf4j.LoggerFactory

class ServiceCommand(val serviceProvider: ServiceProvider) : Command("service", "Manage all your cloud services", "ser") {

    private val logger = LoggerFactory.getLogger(ServiceCommand::class.java)

    private val nameArgument = TextArgument("name")

    init {
        syntax({
            serviceProvider.findAll().forEach { service ->
                logger.info("Service: ${service.name()} | State: ${service.state} | Port: ${service.port}")
            }
        }, TextArgument("list"))

    }
}