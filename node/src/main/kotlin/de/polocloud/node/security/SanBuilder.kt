package de.polocloud.node.security

import de.polocloud.common.utils.localIpAddress
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames

object SanBuilder {

    fun build(spec: NodeIdentitySpec): GeneralNames {
        val names = mutableListOf<GeneralName>()
        spec.dnsNames.forEach    { names += GeneralName(GeneralName.dNSName,   it) }
        spec.ipAddresses.forEach { names += GeneralName(GeneralName.iPAddress, it) }
        return GeneralNames(names.toTypedArray())
    }

    /** SANs for a node certificate. */
    fun forNode(hostname: String, nodeId: String): GeneralNames =
        build(NodeIdentitySpec(
            nodeId     = nodeId,
            dnsNames   = listOf("$nodeId.polocloud.local", "localhost"),
            ipAddresses = listOf(hostname, localIpAddress(), "127.0.0.1"),
        ))

    /** SANs for a CLI client certificate. */
    fun forCliClient(clientIp: String): GeneralNames =
        build(NodeIdentitySpec(
            nodeId      = clientIp,
            dnsNames    = listOf(clientIp, "cli.polocloud.local"),
            ipAddresses = emptyList(),
        ))

    /**
     * SANs for a service instance certificate.
     *
     * Uses `<serviceId>.service.polocloud.local` as the primary DNS SAN so the
     * node can identify which service instance is connecting via the cert CN/SAN.
     */
    fun forService(serviceId: String, planName: String): GeneralNames =
        build(NodeIdentitySpec(
            nodeId    = serviceId,
            dnsNames  = listOf(
                "$serviceId.service.polocloud.local",
                "$planName.service.polocloud.local",
            ),
            ipAddresses = listOf("127.0.0.1"),
        ))
}