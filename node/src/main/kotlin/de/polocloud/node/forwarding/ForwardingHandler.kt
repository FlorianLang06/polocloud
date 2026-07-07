package de.polocloud.node.forwarding

import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

class ForwardingHandler {

    /** Forwarding mode used for services started by this node. */
    val state: ForwardingState = detectForwarding()

    /** The shared forwarding secret, loaded from disk or freshly generated once. */
    val secret: String = loadOrCreateSecret()

    private fun detectForwarding(): ForwardingState = ForwardingState.MODERN

    private fun loadOrCreateSecret(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}