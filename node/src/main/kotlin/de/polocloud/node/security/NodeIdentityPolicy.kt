package de.polocloud.node.security

import de.polocloud.common.utils.localIpAddress
import de.polocloud.common.utils.publicIpAddress

object NodeIdentityPolicy {

    /**
     * @param configuredHostname `general.hostname` of the node this certificate is being
     *   bootstrapped for. Added to the SAN explicitly because [localIpAddress] is a
     *   best-effort guess (first non-loopback IPv4 interface `NetworkInterface` happens to
     *   enumerate) that is not guaranteed to match the address peers actually connect on —
     *   e.g. a machine with a VPN/WSL/Docker adapter can have that guess land on the wrong
     *   interface while `configuredHostname` is the operator-picked, authoritative one.
     */
    fun resolve(nodeId: String, configuredHostname: String? = null): NodeIdentitySpec {
        val dns = mutableListOf(
            "localhost",
            "$nodeId.polocloud.local"
        )

        val ips = mutableListOf("127.0.0.1")

        configuredHostname?.takeIf { it.isNotBlank() && it !in ips }?.let { ips += it }

        // localIpAddress() throws when no non-loopback IPv4 interface is found (e.g. some
        // sandboxed/offline environments); guard it so a head node without one can still
        // bootstrap, just without that SAN entry. Without this, an internal-only cluster
        // (no public IP either) ends up with a head-node certificate whose SAN is only
        // 127.0.0.1 — every peer that connects to the head via its LAN IP then fails TLS
        // hostname verification (gRPC surfaces this as "UNAVAILABLE: io exception").
        runCatching { localIpAddress() }.getOrNull()?.takeIf { it !in ips }?.let { ips += it }
        publicIpAddress()?.takeIf { it !in ips }?.let { ips += it }

        return NodeIdentitySpec(
            nodeId = nodeId,
            dnsNames = dns,
            ipAddresses = ips
        )
    }
}