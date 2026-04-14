package de.polocloud.node.services

import de.polocloud.node.utils.rootDir
import de.polocloud.node.services.process.ServiceProcess
import java.nio.file.Path

class ServiceContainer(val index: Int, val serviceProcess: ServiceProcess) {

    fun name() = serviceProcess.plan + "-" + this.index

    fun path(): Path = rootDir().resolve("local").resolve("services").resolve("instances").resolve(name())

    init {
        // create file system for container
        ServiceTemplateFactory.generateTemplateIfNotExists(this)
    }

}