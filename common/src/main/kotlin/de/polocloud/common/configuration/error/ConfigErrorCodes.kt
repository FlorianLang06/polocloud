package de.polocloud.common.configuration.error

import de.polocloud.common.error.ErrorCode

private fun code(id: Int) = ErrorCode(prefix = "CFG", id = id)

object ConfigErrorCodes {
    val FILE_NOT_FOUND      = code(1)
    val PARSE_FAILED        = code(2)
    val VALIDATION_FAILED   = code(3)
    val MISSING_ANNOTATION  = code(4)
    val NOT_SERIALIZABLE    = code(5)
}