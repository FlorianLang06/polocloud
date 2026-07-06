package de.polocloud.api.services

import de.polocloud.proto.ServiceData

/**
 * Transport-agnostic gateway to the node's service API.
 *
 * Implemented by [GrpcServiceApiClient] for real gRPC traffic; abstracted as an
 * interface so [ServiceService] can be unit-tested without a live node.
 */
interface ServiceApiClient {

    suspend fun findServices(groupFilter: String?, stateFilter: String?): List<ServiceData>
}
