package de.polocloud.node.identity.provider

import java.util.UUID

interface NodeIdProvider {
    fun get(): UUID
}