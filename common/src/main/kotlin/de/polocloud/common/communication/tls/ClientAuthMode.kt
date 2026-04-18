package de.polocloud.common.communication.tls

/**
 * Whether a gRPC server requires a client certificate.
 *
 * This is a transport-agnostic representation of Netty's `ClientAuth` enum.
 * The mapping to the actual Netty type happens in [de.polocloud.common.communication.GrpcEndpoint] so that
 * `common` remains free of Netty/gRPC implementation dependencies.
 *
 * | Mode       | Description                                              |
 * |------------|----------------------------------------------------------|
 * | NONE       | No client certificate required (one-way TLS)             |
 * | OPTIONAL   | Client certificate accepted if presented, not required   |
 * | REQUIRE    | Client certificate is mandatory (full mTLS)              |
 */
enum class ClientAuthMode {
    NONE,
    OPTIONAL,
    REQUIRE,
}