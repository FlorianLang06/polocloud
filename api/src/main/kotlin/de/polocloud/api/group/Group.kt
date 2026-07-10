package de.polocloud.api.group

import de.polocloud.shared.property.Properties
import de.polocloud.shared.property.PropertyHolder

data class Group (
    val name: String,
    val memory: Int,
    val startThreshold: Double,
    val minOnline: Long,
    val maxOnline: Long,
    val platform: String,
    val version: String,
    /** Free-form key/value properties attached to this group (e.g. `fallback=true`). */
    override val properties: Properties = Properties(),
) : PropertyHolder()
