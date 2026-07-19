package de.polocloud.node.services.factory.platform

import kotlinx.serialization.Serializable

/**
 * Where a [PlatformVersion]'s jar comes from.
 *
 * Built-in platform versions (resolved from a remote API by
 * [de.polocloud.node.services.factory.template.PlatformTemplateConverter]) are always [URL].
 * Custom platforms (see `de.polocloud.node.services.factory.platform.custom`) let an operator
 * pick either option when attaching a version.
 */
@Serializable
enum class PlatformVersionSource {
    /** Downloaded from [PlatformVersion.downloadUrl]. */
    URL,
    /** Copied from a jar already present on the node's filesystem at attach time. */
    LOCAL_FILE
}
