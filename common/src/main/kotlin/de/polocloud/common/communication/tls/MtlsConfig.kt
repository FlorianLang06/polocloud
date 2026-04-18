package de.polocloud.common.communication.tls

import java.io.File

/**
 * Immutable value object that bundles all TLS/mTLS material needed to
 * configure either a gRPC server or a gRPC client channel.
 *
 * ---
 * **Server-side** (used by [de.polocloud.common.communication.GrpcEndpoint]):
 * - [certFile]   → server certificate presented to clients
 * - [keyFile]    → matching private key
 * - [caCerts]    → trust anchors; clients signed by ANY of these CAs are accepted
 * - [clientAuth] → NONE (TLS only) | OPTIONAL | REQUIRE (full mTLS)
 *
 * **Client-side** (used by [GrpcChannelFactory]):
 * - [certFile]  → client certificate presented to the server
 * - [keyFile]   → matching private key
 * - [caCerts]   → exactly one CA certificate used to verify the server
 *
 * ---
 * Obtain instances via the companion factory functions rather than the
 * constructor directly:
 *
 * ```kotlin
 * // mTLS (mutual) — used by nodes and services
 * val config = MtlsConfig.mutual(
 *     cert   = CertificateDataStorage.certificateFile(),
 *     key    = CertificateDataStorage.privateKeyFile(),
 *     caCert = CertificateDataStorage.caCertificateFile(),
 * )
 *
 * // Server-only TLS — no client cert required
 * val config = MtlsConfig.serverOnly(
 *     cert   = certFile,
 *     key    = keyFile,
 *     caCert = caCertFile,
 * )
 * ```
 */
data class MtlsConfig(
    /** Certificate file (PEM) presented by this party. */
    val certFile: File,
    /** Private key file (PEM) matching [certFile]. */
    val keyFile: File,
    /**
     * CA certificate files (PEM) used as trust anchors.
     *
     * - Server: clients signed by ANY of these CAs are accepted.
     * - Client: the single CA used to verify the server certificate.
     */
    val caCerts: List<File>,
    /**
     * Whether to require a client certificate on the server side.
     * Ignored when this config is used for a client channel.
     */
    val clientAuth: ClientAuthMode = ClientAuthMode.REQUIRE,
) {

    init {
        require(certFile.exists()) { "Certificate file does not exist: ${certFile.absolutePath}" }
        require(keyFile.exists())  { "Private key file does not exist: ${keyFile.absolutePath}" }
        caCerts.forEach { ca ->
            require(ca.exists()) { "CA certificate file does not exist: ${ca.absolutePath}" }
        }
        require(caCerts.isNotEmpty()) { "At least one CA certificate is required" }
    }

    companion object {

        /**
         * Creates an [MtlsConfig] for **mutual TLS** (both sides authenticate).
         *
         * This is the standard mode for node-to-node and service-to-node communication.
         */
        fun mutual(cert: File, key: File, caCert: File): MtlsConfig =
            MtlsConfig(cert, key, listOf(caCert), ClientAuthMode.REQUIRE)

        /**
         * Creates an [MtlsConfig] for **mutual TLS with multiple trusted CAs**.
         *
         * Use this on the server when you need to accept clients from different
         * certificate authorities on the same port (e.g. CLI CA + Node CA).
         */
        fun mutual(cert: File, key: File, vararg caCerts: File): MtlsConfig =
            MtlsConfig(cert, key, caCerts.toList(), ClientAuthMode.REQUIRE)

        /**
         * Creates an [MtlsConfig] for **server-only TLS** (client is not authenticated).
         *
         * Use this when clients do not yet have a certificate — e.g. during the
         * initial registration handshake where the client obtains its cert.
         */
        fun serverOnly(cert: File, key: File, caCert: File): MtlsConfig =
            MtlsConfig(cert, key, listOf(caCert), ClientAuthMode.NONE)
    }
}
