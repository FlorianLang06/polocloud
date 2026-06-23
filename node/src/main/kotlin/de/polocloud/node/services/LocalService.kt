package de.polocloud.node.services

class LocalService(val service: Service) : Service(service.id, service.index, service.group, service.state) {

    private var process: Process? = null



}