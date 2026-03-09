package de.polocloud.cli.configuration.serialization

import de.polocloud.i18n.model.Language
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.Locale

object LocaleSerializer : KSerializer<Locale> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) {
        encoder.encodeString(Language.code(value))
    }

    override fun deserialize(decoder: Decoder): Locale {
        return Language.of(decoder.decodeString())
    }
}