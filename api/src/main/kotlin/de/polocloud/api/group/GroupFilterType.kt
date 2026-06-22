package de.polocloud.api.group

/**
 * High-level role a group plays in the network.
 *
 * The role is derived from the group's platform: well-known proxy platforms map
 * to [PROXY], everything else is treated as a [SERVER].
 */
enum class GroupFilterType {

    PROXY,
    SERVER;

    /**
     * Returns whether a group running on [platform] belongs to this filter type.
     */
    fun matches(platform: String): Boolean {
        val proxy = platform.lowercase() in PROXY_PLATFORMS
        return when (this) {
            PROXY -> proxy
            SERVER -> !proxy
        }
    }

    companion object {
        private val PROXY_PLATFORMS = setOf("velocity", "bungeecord", "waterfall", "gate", "bungee")
    }
}
