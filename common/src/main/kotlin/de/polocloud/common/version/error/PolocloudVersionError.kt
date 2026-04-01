package de.polocloud.common.version.error

import de.polocloud.common.error.BasePoloError
import de.polocloud.common.error.ErrorSeverity
import de.polocloud.common.error.context.ErrorContext
import kotlinx.serialization.Serializable

sealed class PolocloudVersionError {

    @Serializable
    data class ResourceNotFound(val path: String) : BasePoloError(
        code = VersionErrorCodes.RESOURCE_NOT_FOUND,
        key = "version.resource_not_found",
        placeholders = mapOf("path" to path),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("PolocloudVersionLoader.load", "path" to path),
    )

    @Serializable
    data class MissingProperty(val propertyKey: String) : BasePoloError(
        code = VersionErrorCodes.MISSING_PROPERTY,
        key = "version.missing_property",
        placeholders = mapOf("key" to propertyKey),
        severity = ErrorSeverity.FATAL,
        context = ErrorContext.from("PolocloudVersionLoader.load", "key" to propertyKey),
    )

    @Serializable
    data class InvalidFormat(val input: String) : BasePoloError(
        code = VersionErrorCodes.INVALID_FORMAT,
        key = "version.invalid_format",
        placeholders = mapOf("input" to input),
        severity = ErrorSeverity.WARNING,
        context = ErrorContext.from("PolocloudVersionParser.parse", "input" to input),
    )
}