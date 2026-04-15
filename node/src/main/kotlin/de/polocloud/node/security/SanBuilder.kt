package de.polocloud.node.security

import de.polocloud.common.utils.localIpAddress
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames

object SanBuilder {

    fun build(spec: NodeIdentitySpec): GeneralNames {
        val names = mutableListOf<GeneralName>()

        spec.dnsNames.forEach {
            names += GeneralName(GeneralName.dNSName, it)
        }

        spec.ipAddresses.forEach {
            names += GeneralName(GeneralName.iPAddress, it)
        }

        return GeneralNames(names.toTypedArray())
    }

    /**
     * Builds SANs for a node certificate based on its network identity.
     * Includes: hostname, local IP, loopback, and optional DNS names.
     */
    fun forNode(hostname: String, groupName: String, index: Int): GeneralNames {
        val dnsNames = listOf(
            hostname,
            "$groupName-$index.polocloud.local",
            "localhost"
        )
        val ipAddresses = listOf(
            hostname,
            localIpAddress(),
            "127.0.0.1"
        )

        return build(NodeIdentitySpec(
            nodeName = "$groupName-$index",
            dnsNames = dnsNames,
            ipAddresses = ipAddresses
        ))
    }

    /**
     * Builds SANs for a CLI client certificate.
     * CLI clients only need a DNS name for identification.
     */
    fun forCliClient(clientIp: String): GeneralNames {
        return build(NodeIdentitySpec(
            nodeName = clientIp,
            dnsNames = listOf(clientIp, "cli.polocloud.local"),
            ipAddresses = emptyList()
        ))
    }
}