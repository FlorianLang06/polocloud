package de.polocloud.node.services

import de.polocloud.node.services.control.ServiceControlPlanRepository
import org.slf4j.LoggerFactory

/**
 * Handles discovery, validation, and startup of local services.
 *
 * This class is responsible for:
 * - Scanning available local service definitions
 * - Ensuring each service has an associated control plan
 * - Bootstrapping services based on their configured minimum instances
 *
 * The lifecycle starts with [initialize], which prepares all services
 * and then triggers the startup process.
 */
class ServiceHandler {

    private val logger = LoggerFactory.getLogger(ServiceHandler::class.java)

    /**
     * Cached list of all locally available service definitions.
     */
    private var localServices: List<ServiceHolder> = emptyList()

    /**
     * Cached list of all service container
     */
    private val containers = mutableListOf<ServiceContainer>()

    /**
     * Initializes the service handler.
     *
     * This will:
     * 1. Scan all available services using [ServiceFactory]
     * 2. Ensure each service has a corresponding control plan
     * 3. Generate default plans if missing
     * 4. Start services according to their plans
     */
    fun initialize() {
        localServices = ServiceFactory.scanServices()

        localServices.forEach { service ->
            val plan = ServiceControlPlanRepository.findById(service.name)

            if (plan == null) {
                logger.info("Service '{}' has no control plan. Generating default plan.", service.name)
                ServiceControlPlanRepository.generateDefault(service)
            }
        }

        bootLocal()
    }

    /**
     * Boots all local services based on their control plans.
     *
     * For each plan:
     * - Verifies that a corresponding local service definition exists
     * - Starts the required minimum number of service instances
     *
     * Services without a matching local definition will be skipped.
     */
    fun bootLocal() {
        ServiceControlPlanRepository.localPlans().forEach { plan ->

            val holder = findHolderOrNull(plan.name)

            if (holder == null) {
                logger.warn(
                    "No local service definition found for plan '{}'. Skipping startup.",
                    plan.name
                )
                return@forEach
            }

            repeat(plan.minimum) {
                logger.info("Starting service '{}' (instance {}/{})", plan.name, it + 1, plan.minimum)
                this.containers.add(ServiceFactory.bootService(plan, holder))
            }
        }
    }

    /**
     * Finds a [ServiceHolder] by its identifier.
     *
     * @param id the unique name of the service
     * @return the matching [ServiceHolder], or null if not found
     */
    private fun findHolderOrNull(id: String): ServiceHolder? {
        return localServices.find { it.name == id }
    }

    /**
     * Finds a [ServiceHolder] by its identifier.
     *
     * @param id the unique name of the service
     * @return the matching [ServiceHolder]
     * @throws NoSuchElementException if no matching service is found
     */
    private fun findHolder(id: String): ServiceHolder {
        return localServices.first { it.name == id }
    }
}