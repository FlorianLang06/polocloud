package dev.httpmarco.polocloud.node.grpc

import dev.httpmarco.polocloud.node.cluster.repository.NodeRepository
import dev.httpmarco.polocloud.proto.ApprovalResponse
import dev.httpmarco.polocloud.proto.JoinRequest
import dev.httpmarco.polocloud.proto.NodeServiceGrpcKt
import java.util.UUID

class NodeServiceImpl(val repository: NodeRepository) : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {

    override suspend fun requestApproval(request: JoinRequest): ApprovalResponse {
        val nodeId = UUID.fromString(request.nodeId)
        val publicKeyBase64 = request.publicKey

        val existing = repository.findNode(nodeId)
        if (existing != null) {
            return reject("Node already registered")
        }

        // 2️⃣ PublicKey validieren (Format + Parsebarkeit)
    //    val publicKey = try {
          //  security.parsePublicKey(publicKeyBase64)
      //  } catch (e: Exception) {
          //  return reject("Invalid public key")
      //  }

        // 3️⃣ Optional: NodeID an PublicKey koppeln
        // Beispiel: NodeID = SHA256(publicKey)
      //  if (!security.isNodeIdMatchingPublicKey(nodeId, publicKey)) {
            //return reject("NodeID does not match PublicKey")
     //   }

        // 4️⃣ Join genehmigen → NodeID signieren
       // val signature = security.sign(nodeId.toString().toByteArray())

        return ApprovalResponse.newBuilder()
            .setApproved(true)
         //   .setSignature(signature)
            .build()
    }

    private fun reject(reason: String): ApprovalResponse {
        return ApprovalResponse.newBuilder()
            .setApproved(false)
            .setSignature("")
            .build()
    }

    private fun signNodeId(nodeId: String): String {
        return "signed_$nodeId"
    }
}
