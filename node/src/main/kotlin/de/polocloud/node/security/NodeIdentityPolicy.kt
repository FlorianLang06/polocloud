package de.polocloud.node.security

import de.polocloud.common.utils.localIpAddress
import de.polocloud.common.utils.publicIpAddress

object NodeIdentityPolicy {

    fun resolve(nodeId: String): NodeIdentitySpec {
        val dns = mutableListOf(
            "localhost",
            "$nodeId.polocloud.local"
        )

        val ips = mutableListOf("127.0.0.1")

        // localIpAddress() throws when no non-loopback IPv4 interface is found (e.g. some
        // sandboxed/offline environments); guard it so a head node without one can still
        // bootstrap, just without that SAN entry. Without this, an internal-only cluster
        // (no public IP either) ends up with a head-node certificate whose SAN is only
        // 127.0.0.1 — every peer that connects to the head via its LAN IP then fails TLS
        // hostname verification (gRPC surfaces this as "UNAVAILABLE: io exception").
        runCatching { localIpAddress() }.getOrNull()?.let { ips += it }
        publicIpAddress()?.let { ips += it }

        return NodeIdentitySpec(
            nodeId = nodeId,
            dnsNames = dns,
            ipAddresses = ips
        )
    }
}