package de.polocloud.node.core.configuration

import de.polocloud.common.Address
import de.polocloud.common.GLOBAL_ADDRESS
import de.polocloud.node.core.configuration.serializer.LocaleSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GeneralConfiguration(
    @Serializable(with = LocaleSerializer::class) var locale: Locale = Locale.US,
    var bindAddress: Address = GLOBAL_ADDRESS.withPort(4240),
)
