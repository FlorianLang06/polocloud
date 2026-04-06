package de.polocloud.node.services

import org.slf4j.LoggerFactory

object ServiceHandler {

    private val logger = LoggerFactory.getLogger(ServiceHandler::class.java)
    private var localServices : List<ServiceHolder> = emptyList()

    fun initialize() {
        this.localServices = ServiceFactory.scanServices()
        logger.info("Loaded local services from cache ${localServices.size}")
    }
}