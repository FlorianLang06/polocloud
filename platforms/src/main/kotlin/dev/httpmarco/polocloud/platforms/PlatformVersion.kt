package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
data class PlatformVersion(val version: String, val buildId: String, val requiredRuntimeVersion: String? = null) {


}