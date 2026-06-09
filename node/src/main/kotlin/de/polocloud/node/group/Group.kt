package de.polocloud.node.group

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName

@RepositoryName("groups")
data class Group (
    @EntryIdentifier val name: String,
    val memory: String,
    val startThreshold: Double,
    val minOnline: Long,
    val maxOnline: Long,
    private val platform: String,
    private val version: String
) {

}