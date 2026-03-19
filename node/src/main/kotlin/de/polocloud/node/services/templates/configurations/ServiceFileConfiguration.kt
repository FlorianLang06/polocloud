package de.polocloud.node.services.templates.configurations

import kotlinx.serialization.Serializable

@Serializable
data class ServiceFileConfiguration(
    val copyAll: Boolean,
)