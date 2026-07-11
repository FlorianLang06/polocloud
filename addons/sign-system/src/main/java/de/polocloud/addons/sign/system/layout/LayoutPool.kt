package de.polocloud.addons.sign.system.layout

import de.polocloud.addons.sign.system.SignType
import de.polocloud.shared.service.ServiceState

class LayoutPool {

    private val layout = linkedSetOf<Layout>()

    init {
        val defaultLayout = SignLayout("default")
        defaultLayout.appendFrame(ServiceState.UNKNOWN, LayoutFrame(listOf("§8⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫", "Server", "wird gesucht", "§8⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫ ⚫")))

        this.layout.add(defaultLayout)
    }

    fun findSign(id: String) : SignLayout {
        return this.layout.stream().filter { it.id == id }.filter { it.type == SignType.SIGN }.findFirst().orElse(null) as SignLayout
    }
}