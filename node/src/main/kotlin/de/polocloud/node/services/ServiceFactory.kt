package de.polocloud.node.services

import de.polocloud.node.services.process.ServiceProcess
import de.polocloud.node.services.process.ServiceProcessRepository

class ServiceFactory(val serviceProcessRepository: ServiceProcessRepository) {

    fun bootService() {



        val process = ProcessBuilder().start()


        process.pid()
    }

}