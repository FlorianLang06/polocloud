package de.polocloud.cli.configuration

import de.polocloud.cli.configuration.serialization.LocaleSerializer
import dev.httpmarco.polocloud.i18n.model.Language
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class CliConfiguration(
    @Serializable(with = LocaleSerializer::class)
    val locale: Locale = Language.of("en_US")
)