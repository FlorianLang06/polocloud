package de.polocloud.addons.sign.system

import de.polocloud.addons.sign.system.layout.Layout
import de.polocloud.shared.service.Service
import org.bukkit.Material

data class SignData(
    val type: SignType,
    var service: Service?,
    val group: String,
    val position: SignPosition,
    val layout: Layout
)