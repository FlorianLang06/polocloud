package de.polocloud.common.communication.security

import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.StringWriter

fun PKCS10CertificationRequest.toPem(): String {
    return StringWriter().use { writer ->
        JcaPEMWriter(writer).use { it.writeObject(this) }
        writer.toString()
    }
}