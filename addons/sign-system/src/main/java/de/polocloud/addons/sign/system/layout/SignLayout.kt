package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignType
import de.polocloud.shared.service.ServiceState
import java.util.LinkedList

class SignLayout(id: String) : Layout(id, SignType.SIGN) {

    val frames = hashMapOf<ServiceState, LinkedList<LayoutFrame>>()

    fun appendFrame(state: ServiceState, frame : LayoutFrame) {
        this.frames[state] = listOf(frame).toCollection(LinkedList())
    }
}