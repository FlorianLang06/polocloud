package de.polocloud.node.services.factory.platform.custom

import de.polocloud.node.services.factory.platform.PlatformVersionSource
import kotlinx.serialization.Serializable

/**
 * A single version attached to a [CustomPlatform], nested inside its `versionsJson` column
 * (see [CustomPlatformVersionCodec]) rather than being its own database entity.
 *
 * @param version  Operator-chosen version label (e.g. "1.0.0") — unique within its platform.
 * @param source   Whether [location] is a download URL or a node-local file path.
 * @param location The URL that was validated reachable, or the local file path that was
 *                  validated to exist, at the time this version was attached
 *                  ([CustomPlatformService.addVersion]). Kept for diagnostics — the jar itself
 *                  already lives in [de.polocloud.node.services.factory.process.PlatformJarCache].
 */
@Serializable
data class CustomPlatformVersion(
    val version: String,
    val source: PlatformVersionSource,
    val location: String
)
