package de.polocloud.addons.sign.system

import de.polocloud.addons.sign.system.layout.LayoutPool
import de.polocloud.addons.sign.system.spigot.BukkitSignPlatform
import de.polocloud.api.Polocloud
import de.polocloud.api.event.subscribe
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.service.Service

object SignSystem {

    private val platform: SignPlatform = BukkitSignPlatform()
    private val signPool = SignPool()
    private val layoutPool = LayoutPool()


    init {
        Polocloud.eventService.subscribe<ServerStartedEvent> { event ->

        }
        Polocloud.eventService.subscribe<ServerStoppedEvent> { event ->

        }
    }

    fun attachSign(type: SignType, x: Int, y: Int, z: Int, world: String, group: String) {
        val data = SignData(type, null, group, SignPosition(x, y, z, world), layoutPool.findSign("default"))

        this.signPool.attach(data)
        this.updateSign(data)
    }

    fun updateSign(data: SignData) {
        platform.displaySign(data)
    }

    fun findEmptySign(service: Service) {

    }
}