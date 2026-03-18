package de.polocloud.node.registration

import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.generator.CSPRNGGenerator
import de.polocloud.proto.RegisterNodeResponse
import java.util.UUID

class RegistrationManager(config: ClusterConfiguration) {

    val publicRegistrationToken = CSPRNGGenerator.generate()
    private var registrationServer = RegistrationServer(config.registration)

    fun allowRequests() {
        this.registrationServer.allowRequests()
    }

    fun tryJoinCluster(registrationInfo: RegistrationInfo, localId : UUID) {
        val client = RegistrationClient()
        val response = client.tryRegister(registrationInfo, localId, "");
    }
}