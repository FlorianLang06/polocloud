package de.polocloud.node.core.context

import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.services.ServiceHandler

class NodeRuntimeContext(
    val localNodeContainer: LocalNodeContainer,
    val registrationManager: RegistrationManager,
    val serviceHandler: ServiceHandler,
    val grpcEndpoint: NodeGrpcEndpoint,
    val headNodeConnection: NodeGrpcClient?
)