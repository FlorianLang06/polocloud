package de.polocloud.node.group

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName

@RepositoryName("groups")
data class Group (
    @EntryIdentifier val name: String,
    val memory: Int,
    val startThreshold: Double,
    val minOnline: Long,
    val maxOnline: Long,
    val platform: String,
    val version: String,
    var static: Boolean = false,
    /**
     * Free-form key/value properties (e.g. `fallback=true`), persisted as JSON.
     *
     * Stored as a JSON string because the SQL layer maps each field to one column
     * and cannot persist a `Map` directly. Read [properties] for the decoded view.
     */
    val propertiesJson: String = "{}",
) {

    /**
     * The decoded property map. Computed (no backing field) so it is not turned into
     * its own SQL column; the persisted representation is [propertiesJson].
     */
    val properties: MutableMap<String, String>
        get() = PropertyCodec.decode(propertiesJson)
}