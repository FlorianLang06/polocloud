package de.polocloud.node.registration

import de.polocloud.proto.NodeRegistrationServiceGrpcKt
import de.polocloud.proto.RegisterNodeRequest
import de.polocloud.proto.RegisterNodeResponse

class RegistrationService :   NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineImplBase() {

    override suspend fun registerNode(request: RegisterNodeRequest): RegisterNodeResponse {
        System.out.println("peod")
        return RegisterNodeResponse.newBuilder().build()
    }
}