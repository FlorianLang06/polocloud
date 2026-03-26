package de.polocloud.common.configuration.error

import de.polocloud.common.error.BasePoloError
import kotlinx.serialization.Serializable
import de.polocloud.common.error.context.ErrorContext
import de.polocloud.common.error.ErrorSeverity

sealed class ConfigurationError {

    @Serializable
    data class ParseFailed(val file: String, val reason: String) : BasePoloError(
        code     = ConfigErrorCodes.PARSE_FAILED,
        key      = "configuration.parse_failed",
        placeholders = mapOf(
            "file" to file,
            "reason" to reason
        ),
        severity = ErrorSeverity.CRITICAL,
        context  = ErrorContext.from("ConfigHolder.loadFromDisk", "file" to file),
    )

    @Serializable
    data class ValidationFailed(val file: String, val field: String, val reason: String) : BasePoloError(
        code     = ConfigErrorCodes.VALIDATION_FAILED,
        key      = "configuration.validation_failed",
        placeholders = mapOf(
            "field" to field,
            "file" to file,
            "reason" to reason
        ),
        severity = ErrorSeverity.CRITICAL,
        context  = ErrorContext.from("ConfigHolder.loadFromDisk", "file" to file, "field" to field),
    )

    @Serializable
    data class MissingAnnotation(val className: String) : BasePoloError(
        code     = ConfigErrorCodes.MISSING_ANNOTATION,
        key      = "configuration.missing_annotation",
        placeholders = mapOf(
            "className" to className,
        ),
        severity = ErrorSeverity.FATAL,
        context  = ErrorContext.from("ConfigManager.load", "class" to className),
    )

    @Serializable
    data class NotSerializable(val className: String) : BasePoloError(
        code     = ConfigErrorCodes.NOT_SERIALIZABLE,
        key      = "configuration.not_serializable",
        placeholders = mapOf(
            "className" to className,
        ),
        severity = ErrorSeverity.FATAL,
        context  = ErrorContext.from("ConfigHolder.resolveSerializer", "class" to className),
    )
}