package de.polocloud.node.security

import de.polocloud.common.utils.publicIpAddress

object NodeIdentityPolicy {

    fun resolve(nodeId: String): NodeIdentitySpec {
        val dns = mutableListOf(
            "localhost",
            "$nodeId.polocloud.local"
        )

        val ips = mutableListOf("127.0.0.1")

        //localIpAddress().let { ips += it } This does not work
        publicIpAddress()?.let { ips += it }

        return NodeIdentitySpec(
            nodeId = nodeId,
            dnsNames = dns,
            ipAddresses = ips
        )
    }
}