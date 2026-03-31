package de.polocloud.common.version.error

import de.polocloud.common.error.ErrorCode

private fun code(id: Int) = ErrorCode(prefix = "VER", id = id)

object VersionErrorCodes {
    val RESOURCE_NOT_FOUND = code( 1)
    val MISSING_PROPERTY = code(2)
    val INVALID_FORMAT = code(3)
}