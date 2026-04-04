package de.polocloud.node.error

import de.polocloud.common.error.ErrorCode

private fun code(id: Int) = ErrorCode(prefix = "NODE", id = id)

object NodeErrorCodes {
    val DATABASE_CONNECTION_FAILED = code(1)
    val NOT_REGISTERED_IN_CLUSTER = code(2)
    val REGISTRATION_FAILED = code(3)
    val HEARTBEAT_SAVE_FAILED = code(4)
    val SESSION_CLEANUP_FAILED = code(5)
}