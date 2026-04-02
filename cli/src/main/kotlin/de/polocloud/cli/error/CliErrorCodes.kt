package de.polocloud.cli.error

import de.polocloud.common.error.ErrorCode

private fun code(id: Int) = ErrorCode("CLI", id)

object CliErrorCodes {
    val REGISTRATION_DENIED = code(1)
    val REGISTRATION_FAILED = code(2)
}