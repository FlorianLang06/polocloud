package de.polocloud.node.security

data class NodeIdentitySpec(
    val nodeName: String,
    val dnsNames: List<String>,
    val ipAddresses: List<String>
)