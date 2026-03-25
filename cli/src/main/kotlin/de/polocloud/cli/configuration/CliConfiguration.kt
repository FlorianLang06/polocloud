package de.polocloud.cli.configuration

import de.polocloud.cli.configuration.serialization.LocaleSerializer
import de.polocloud.common.configuration.ConfigFile
import de.polocloud.common.configuration.DefaultableConfig
import de.polocloud.i18n.model.Language
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
@ConfigFile("polocloud-cli.json")
data class CliConfiguration(
    @Serializable(with = LocaleSerializer::class)
    val locale: Locale = Language.of("en_US")
) {
    companion object : DefaultableConfig<CliConfiguration> {
        override fun createDefault(): CliConfiguration {
            return CliConfiguration()
        }

    }
}