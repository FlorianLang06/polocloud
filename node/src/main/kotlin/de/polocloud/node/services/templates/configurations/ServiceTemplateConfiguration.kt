package de.polocloud.node.services.templates.configurations

import kotlinx.serialization.Serializable

@Serializable
data class ServiceTemplateConfiguration(
    val name: String,
    val mainJar: String,
    val description: String? = null,
    val version: String,

    val runtime: ServiceRuntimeConfiguration,
    val files: ServiceFileConfiguration
)