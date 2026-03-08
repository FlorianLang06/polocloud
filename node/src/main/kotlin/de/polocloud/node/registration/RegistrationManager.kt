package de.polocloud.node.registration

import de.polocloud.node.security.ClusterSecurity

object RegistrationManager {

    fun registerNode(info: RegistrationInfo, security: ClusterSecurity) {
        val client = RegistrationClient(security)
        val response = client.register(info)

        println("Registration successful: ${response.accepted}, Message: ${response.message}")
    }
}