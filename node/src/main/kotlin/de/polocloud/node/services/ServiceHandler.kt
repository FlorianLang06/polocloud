package de.polocloud.node.services

import de.polocloud.node.services.control.ServiceControlPlanRepository
import org.slf4j.LoggerFactory

class ServiceHandler {

    private val logger = LoggerFactory.getLogger(ServiceHandler::class.java)
    private var localServices: List<ServiceHolder> = emptyList()

    fun initialize() {
        this.localServices = ServiceFactory.scanServices()

        this.localServices.forEach {
            val plan = ServiceControlPlanRepository.findById(it.name)

            if(plan == null) {
                logger.info("The service ${it.name} has no process plan. Creating a new one...")
                ServiceControlPlanRepository.generateDefault(it)
            }
        }
        this.bootLocal()
    }

    fun bootLocal() {
        ServiceControlPlanRepository.localPlans().forEach {
            for (i in 0 until it.minimum) {
                logger.info("Start a new service: ${it.name}....")
                ServiceFactory.bootService(it)
            }
        }
    }
}