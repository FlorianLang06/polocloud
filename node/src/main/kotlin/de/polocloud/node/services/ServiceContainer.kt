package de.polocloud.node.services

import de.polocloud.node.services.process.ServiceProcess

class ServiceContainer(val index: Int, val serviceProcess: ServiceProcess) {

    fun name() = "event-" + this.index


}