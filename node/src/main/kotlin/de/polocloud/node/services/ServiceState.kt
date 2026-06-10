package de.polocloud.node.services

enum class ServiceState {
    QUEUED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}
