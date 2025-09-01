package dev.httpmarco.polocloud.bridge.api

import dev.httpmarco.polocloud.sdk.java.Polocloud
import dev.httpmarco.polocloud.shared.PolocloudShared
import dev.httpmarco.polocloud.shared.events.Event
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangeStateEvent
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceState

abstract class BridgeInstance<T, R> {

    abstract fun generateInfo(service: Service): T

    abstract fun findInfo(name: String): T?

    abstract fun playerCount(identifier: R): Int

    private lateinit var polocloud: PolocloudShared

    protected val registeredFallbacks = ArrayList<R>()

    abstract fun registerService(identifier: T, fallback: Boolean = false): R

    abstract fun unregisterService(identifier: T): R

    fun initialize(polocloud: PolocloudShared) {
        this.polocloud = polocloud
        polocloud.serviceProvider().findByType(GroupType.SERVER).forEach {
            if(it.state !== ServiceState.ONLINE) {
                return@forEach
            }
            registerService(generateInfo(it), isFallback(it))
        }


        polocloud.eventProvider().subscribe(ServiceChangeStateEvent::class.java) { event ->
            val service = event.service

            if (service.state == ServiceState.ONLINE) {
                if (service.type == GroupType.SERVER) {
                    registerServiceInternal(service)
                }
            }

            if (service.state == ServiceState.STOPPING) {
                unregisterServiceInternal(service)
            }
        }
    }

    fun initialize() {
        this.initialize(Polocloud.instance())
    }

    fun updatePolocloudPlayer(event: Event) {
        polocloud.eventProvider().call(event)
    }

    fun fallbackServer(): R {
        val withPlayers = ArrayList<Pair<R, Int>>()
        for (fallback in registeredFallbacks) {
            withPlayers.add(fallback to playerCount(fallback))
        }

        val (service) = withPlayers.minBy { it.second }

        return service
    }

    private fun isFallback(service: Service): Boolean {
        return service.properties["fallback"]?.equals("true", ignoreCase = true) == true
    }

    private fun registerServiceInternal(service: Service) {
        val fallback = isFallback(service)
        val registeredService = registerService(generateInfo(service), fallback)

        if (fallback) {
            registeredFallbacks.add(registeredService)
        }
    }

    private fun unregisterServiceInternal(service: Service) {
        findInfo(service.name())?.let { info ->
            val result = unregisterService(info)
            registeredFallbacks.remove(result)
        }!!
    }
}