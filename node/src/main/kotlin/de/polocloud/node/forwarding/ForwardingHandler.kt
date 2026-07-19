package de.polocloud.node.forwarding

import de.polocloud.node.utils.rootDir
import java.io.File
import java.nio.file.Files
import java.util.UUID

class ForwardingHandler {

    // Resolved lazily, not at construction: FactoryService (and therefore this class) is
    // built well before rootDir() is guaranteed to be configured (e.g. in unit tests that
    // construct a ServiceProvider directly), and eagerly touching it here would fail those
    // callers even though they never read [secret].
    private val secretFile: File by lazy { rootDir().resolve(".cache/identity/forwarding-secret.txt").toFile() }

    /** Forwarding mode used for services started by this node. */
    val state: ForwardingState = detectForwarding()

    /**
     * The shared forwarding secret, loaded from disk or freshly generated once.
     *
     * Only the very first node of a cluster (the one that generates it here) is the
     * source of truth; every node that joins an existing cluster overwrites this with
     * the real value via [adopt] — see
     * [de.polocloud.node.identity.NodeIdentityService.adoptForwardingSecret].
     */
    var secret: String = ""
        get() {
            if (field.isEmpty()) {
                field = loadOrCreateSecret()
            }
            return field
        }
        private set

    private fun detectForwarding(): ForwardingState = ForwardingState.MODERN

    private fun loadOrCreateSecret(): String {
        if (secretFile.exists()) {
            return secretFile.readText().trim()
        }

        val generated = UUID.randomUUID().toString().replace("-", "")
        persist(generated)
        return generated
    }

    /**
     * Replaces the local forwarding secret with the cluster's real one, received from a
     * peer that already holds it (see `NodeService.FetchForwardingSecret`), so this
     * node's services agree with the rest of the cluster instead of the locally
     * generated placeholder.
     */
    fun adopt(newSecret: String) {
        if (newSecret == secret) return

        secret = newSecret
        persist(newSecret)
    }

    private fun persist(value: String) {
        Files.createDirectories(secretFile.toPath().parent)
        secretFile.writeText(value)
    }
}