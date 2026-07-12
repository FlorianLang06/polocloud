package de.polocloud.api.group

import de.polocloud.shared.property.Properties

/**
 * Fluent builder used to create or edit a [Group].
 *
 * Obtain an instance from [GroupService.create] (name pre-filled) or inside the
 * [GroupService.edit] editor. Configure the fields, then call [submit] to send
 * the change to the cluster — [submit] returns the persisted group.
 */
class GroupBuilder internal constructor(
    private val submitter: (Group) -> Group,
) {

    private var name: String = ""
    private var memory: Int = 512
    private var startThreshold: Double = 0.0
    private var minOnline: Long = 0
    private var maxOnline: Long = 1
    private var platform: String = ""
    private var version: String = ""
    private val properties: Properties = Properties()
    private val nodes: MutableList<String> = mutableListOf()

    fun name(name: String): GroupBuilder = apply { this.name = name }
    fun memory(memory: Int): GroupBuilder = apply { this.memory = memory }
    fun startThreshold(startThreshold: Double): GroupBuilder = apply { this.startThreshold = startThreshold }
    fun minOnline(minOnline: Long): GroupBuilder = apply { this.minOnline = minOnline }
    fun maxOnline(maxOnline: Long): GroupBuilder = apply { this.maxOnline = maxOnline }
    fun platform(platform: String): GroupBuilder = apply { this.platform = platform }
    fun version(version: String): GroupBuilder = apply { this.version = version }

    /** Sets a single key/value property on the group. */
    fun property(key: String, value: String): GroupBuilder = apply { this.properties.set(key, value) }

    /** Adds all entries of [properties] to the group's properties. */
    fun properties(properties: Map<String, String>): GroupBuilder =
        apply { properties.forEach { (key, value) -> this.properties.set(key, value) } }

    /** Marks this group as a fallback target for the bridge (`fallback=true`). */
    fun fallback(fallback: Boolean = true): GroupBuilder =
        apply { this.properties.set(Properties.FALLBACK, fallback.toString()) }

    /**
     * Marks this group as a fallback target and ranks it against other fallback
     * groups: when a proxy has to pick among several, higher [priority] values are
     * tried first. Implies [fallback].
     */
    fun fallbackPriority(priority: Int): GroupBuilder =
        apply {
            this.properties.set(Properties.FALLBACK, true.toString())
            this.properties.set(Properties.FALLBACK_PRIORITY, priority.toString())
        }

    /** Restricts this group to a single node (by its cluster name, e.g. `node-1`). */
    fun node(name: String): GroupBuilder = apply { this.nodes.add(name) }

    /**
     * Restricts this group to the given nodes (by their cluster names). Leave unset (or
     * empty) to allow the group to start on any online node.
     */
    fun nodes(vararg names: String): GroupBuilder = apply { this.nodes.addAll(names) }

    internal fun toGroup(): Group {
        require(name.isNotBlank()) { "Group name must be set" }
        return Group(name, memory, startThreshold, minOnline, maxOnline, platform, version, properties, nodes.toList())
    }

    /**
     * Sends the configured group to the cluster and returns the persisted group.
     */
    fun submit(): Group = submitter(toGroup())
}
