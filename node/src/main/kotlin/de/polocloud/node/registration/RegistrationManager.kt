package de.polocloud.node.registration

import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.generator.CSPRNGGenerator
import de.polocloud.node.generator.CertificateSigningRequestGenerator
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateAuthority
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.proto.RegisterNodeResponse
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.io.StringWriter
import java.security.KeyPair
import java.util.UUID

class RegistrationManager(config: ClusterConfiguration, repository: NodeRepository, keyPair: KeyPair) {

    val publicRegistrationToken = CSPRNGGenerator.generate()

    private var registrationServer = RegistrationServer(this,config.registration, repository, keyPair)

    fun allowRequests() {
        this.registrationServer.allowRequests()
    }

    fun tryJoinCluster(registrationInfo: RegistrationInfo, localId : UUID, certificateDataStorage : CertificateDataStorage) {
        val client = RegistrationClient()
        val response = client.tryRegister(registrationInfo, localId, csrToPem(CertificateSigningRequestGenerator(certificateDataStorage.keyPair, localId).generate()))

        System.out.println(response.caCertificate)
        System.err.println(response.certificate)
    }

    fun csrToPem(csr: PKCS10CertificationRequest): String {
        val writer = StringWriter()
        JcaPEMWriter(writer).use {
            it.writeObject(csr)
        }
        return writer.toString()
    }
}