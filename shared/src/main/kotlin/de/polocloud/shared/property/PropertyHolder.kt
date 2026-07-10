package de.polocloud.shared.property

/**
 * Common base for any cluster object that carries [Properties] — currently groups
 * and services.
 *
 * The user-facing motivation is a single, shared "property object" on both groups
 * and services (this is the abstract class that unifies them), so higher layers can
 * read well-known flags such as [isFallback] uniformly regardless of what they hold.
 */
abstract class PropertyHolder {

    abstract val properties: Properties

    /** Whether this holder is flagged as a fallback target (see [Properties.FALLBACK]). */
    fun isFallback(): Boolean = properties.getBoolean(Properties.FALLBACK)
}
