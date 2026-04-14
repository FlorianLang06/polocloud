package de.polocloud.common.communication.certificate

import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.StringReader
import java.io.StringWriter
import java.security.cert.X509Certificate

fun parseCsr(csrPem: String): PKCS10CertificationRequest {
    PEMParser(StringReader(csrPem)).use { parser ->
        val obj = parser.readObject()
        return obj as PKCS10CertificationRequest
    }
}

fun certToPem(cert: X509Certificate): String {
    val writer = StringWriter()
    JcaPEMWriter(writer).use { it.writeObject(cert) }
    return writer.toString()
}