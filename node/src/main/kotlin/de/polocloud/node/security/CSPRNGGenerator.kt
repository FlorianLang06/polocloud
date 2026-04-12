package de.polocloud.node.security

import de.polocloud.common.generator.Generator
import java.security.SecureRandom
import java.util.Base64

object CSPRNGGenerator : Generator<String> {

    private val secureRandom = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}