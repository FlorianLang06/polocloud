package de.polocloud.addons.sign.system

import java.util.UUID

abstract class AbstractConnector {

    abstract fun update(data: SignData)

    abstract fun setup(data: SignData)

    abstract fun delete(data: SignData)

    abstract fun onView(data: SignData, player: UUID)
}