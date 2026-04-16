package de.polocloud.services.sdk

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class ServiceBoot

private val logger: Logger = LogManager.getLogger(ServiceBoot::class.java)

fun main() {
    System.setProperty("PID", ProcessHandle.current().pid().toString())

    val serviceId = System.getProperty("service.id")
    val serviceName = System.getProperty("service.name")

    logger.info("Starting service: $serviceName (id: $serviceId)")
    logger.info("Log directory: services/logs")

    while (true) {
        Thread.sleep(1000)
    }
}