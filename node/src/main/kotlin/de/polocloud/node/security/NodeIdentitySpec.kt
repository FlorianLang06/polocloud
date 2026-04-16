package de.polocloud.node.security

data class NodeIdentitySpec(
    val nodeId: String,
    val dnsNames: List<String>,
    val ipAddresses: List<String>
)