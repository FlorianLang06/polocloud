package de.polocloud.shared.property

import kotlinx.serialization.Serializable

/**
 * A small, serializable key/value container attached to every [PropertyHolder]
 * (groups and services).
 *
 * Values are stored as strings and travel over gRPC as a `map<string, string>`
 * (see the `properties` field on `GroupData` / `ServiceData`). Typed helpers
 * ([getBoolean], [getInt]) parse on read so callers never deal with raw strings.
 *
 * Backed by a [LinkedHashMap] so iteration order stays stable (useful when the
 * properties are printed in the CLI).
 */
@Serializable
class Properties(
    private val values: MutableMap<String, String> = LinkedHashMap(),
) {

    companion object {
        /** Well-known property marking a group as a fallback target for the bridge. */
        const val FALLBACK = "fallback"

        /** Builds [Properties] from a plain map (e.g. decoded from protobuf). */
        fun of(map: Map<String, String>): Properties = Properties(LinkedHashMap(map))
    }

    /** The raw value stored under [key], or `null` if absent. */
    operator fun get(key: String): String? = values[key]

    /** The raw value stored under [key], or [default] if absent. */
    fun getOrDefault(key: String, default: String): String = values[key] ?: default

    /** Parses the value under [key] as a boolean, defaulting to [default] when absent/blank. */
    fun getBoolean(key: String, default: Boolean = false): Boolean =
        values[key]?.toBooleanStrictOrNull() ?: default

    /** Parses the value under [key] as an int, defaulting to [default] when absent/unparsable. */
    fun getInt(key: String, default: Int = 0): Int = values[key]?.toIntOrNull() ?: default

    /** Whether a value is stored under [key]. */
    fun has(key: String): Boolean = values.containsKey(key)

    /** Stores [value] under [key], replacing any previous value. Returns this holder for chaining. */
    fun set(key: String, value: String): Properties = apply { values[key] = value }

    /** Removes the value stored under [key], if any. */
    fun remove(key: String): Properties = apply { values.remove(key) }

    /** An immutable snapshot of all entries (used when mapping to protobuf). */
    fun asMap(): Map<String, String> = LinkedHashMap(values)

    /** Whether no properties are set. */
    fun isEmpty(): Boolean = values.isEmpty()

    /** Returns a copy of these properties with [key]=[value] additionally set. */
    fun copyWith(key: String, value: String): Properties =
        Properties(LinkedHashMap(values)).set(key, value)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Properties && other.values == values)

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = values.toString()
}
