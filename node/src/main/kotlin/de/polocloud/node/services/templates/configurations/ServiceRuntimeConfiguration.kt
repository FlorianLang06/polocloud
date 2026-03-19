package de.polocloud.node.services.templates.configurations

import kotlinx.serialization.Serializable

@Serializable
data class ServiceRuntimeConfiguration(
    val memory: Int,
    val jvmArgs: List<String>,
)