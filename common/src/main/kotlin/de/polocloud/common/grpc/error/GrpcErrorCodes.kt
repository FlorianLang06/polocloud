package de.polocloud.common.grpc.error

import de.polocloud.common.error.ErrorCode

private fun code(id: Int) = ErrorCode(prefix = "GRPC", id = id)

object GrpcErrorCodes {
    val BIND_FAILED = code(1)
    val TLS_SETUP_FAILED = code(2)
    val SHUTDOWN_TIMEOUT = code(3)
}