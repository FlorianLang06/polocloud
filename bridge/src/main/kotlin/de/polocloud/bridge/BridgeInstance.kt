package de.polocloud.bridge

import de.polocloud.api.services.Service

abstract class BridgeInstance<T> {

    abstract fun registerService(info: T, service: Service)

    abstract fun unregisterService(info: T, service: Service)

    abstract fun mapService(service: Service): T

}