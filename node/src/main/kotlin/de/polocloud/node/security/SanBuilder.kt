package de.polocloud.node.security

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
}