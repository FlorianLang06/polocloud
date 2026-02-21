package dev.httpmarco.polocloud.cli.config

import kotlinx.serialization.Serializable

@Serializable
data class InstallerConfig(
    val createdAt: String? = null,
    val language: String = "en_US",
    val module: String? = null,
    val database: Database = Database(),
    val redis: Redis = Redis(),
    val cluster: Boolean = false
) {

    @Serializable
    data class Database(
        val enabled: Boolean = false,
        val type: String? = null,
        val engine: String? = null,
        val credentialsRef: String? = null
    )

    @Serializable
    data class Redis(
        val enabled: Boolean = false,
        val credentialsRef: String? = null
    )
}