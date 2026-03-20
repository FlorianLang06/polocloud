package de.polocloud.common.certificate

import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.StringReader

fun parseCsr(csrPem: String): PKCS10CertificationRequest {
    PEMParser(StringReader(csrPem)).use { parser ->
        val obj = parser.readObject()
        return obj as PKCS10CertificationRequest
    }
}

fun certToPem(cert: java.security.cert.X509Certificate): String {
    val writer = java.io.StringWriter()
    JcaPEMWriter(writer).use { it.writeObject(cert) }
    return writer.toString()
}