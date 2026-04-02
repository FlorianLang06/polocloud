package de.polocloud.cli.configuration

import de.polocloud.cli.configuration.serialization.LocaleSerializer
import de.polocloud.common.Address
import de.polocloud.common.configuration.ConfigurationFile
import de.polocloud.common.configuration.DefaultableConfiguration
import de.polocloud.i18n.model.Language
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
@ConfigurationFile("polocloud-cli.json")
data class CliConfiguration(
    @Serializable(with = LocaleSerializer::class)
    val locale: Locale = Language.of("en_US"),
    val nodeAddress: Address? = null,
) {
    companion object : DefaultableConfiguration<CliConfiguration> {
        override fun createDefault(): CliConfiguration {
            return CliConfiguration()
        }

    }
}