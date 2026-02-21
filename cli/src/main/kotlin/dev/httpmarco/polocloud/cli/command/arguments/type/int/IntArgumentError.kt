package dev.httpmarco.polocloud.cli.command.arguments.type.int

/**
 * Represents the possible validation errors for an [IntArgument].
 */
enum class IntArgumentError {

    /**
     * The input string could not be parsed as an integer.
     */
    INVALID_INT,

    /**
     * The parsed integer is below the configured [IntArgument.minValue].
     */
    LOWER_THAN_MIN,

    /**
     * The parsed integer exceeds the configured [IntArgument.maxValue].
     */
    HIGHER_THAN_MAX
}