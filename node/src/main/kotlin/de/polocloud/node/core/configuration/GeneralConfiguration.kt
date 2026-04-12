package de.polocloud.node.core.configuration

import de.polocloud.common.Address
import de.polocloud.common.LOCALHOST_ADDRESS
import de.polocloud.node.core.configuration.serializer.LocaleSerializer
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class GeneralConfiguration(
    @Serializable(with = LocaleSerializer::class) var locale: Locale = Locale.US,
    var bindAddress: Address = LOCALHOST_ADDRESS.withPort(4239),
)
