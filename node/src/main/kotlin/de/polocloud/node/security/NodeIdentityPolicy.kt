package de.polocloud.node.security

import de.polocloud.common.utils.localIpAddress

object NodeIdentityPolicy {

    fun resolve(hostname: String): NodeIdentitySpec {
        val dns = mutableListOf(
            "localhost",
            "$hostname.polocloud.local"
        )

        val ips = mutableListOf<String>()

        localIpAddress().let { ips += it }

        return NodeIdentitySpec(
            nodeName = hostname,
            dnsNames = dns,
            ipAddresses = ips
        )
    }
}