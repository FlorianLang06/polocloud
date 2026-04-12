package de.polocloud.node.cluster.node

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import java.util.UUID

object NodeRepository {

    private val nodeDatabaseKey = DatabaseKey(NodeData::class)

    fun find(id: UUID) = DatabaseAccess.executor().findById(nodeDatabaseKey, id)

    fun save(node: NodeData) = DatabaseAccess.executor().save(nodeDatabaseKey, node)

    fun findAll() = DatabaseAccess.executor().findAll(nodeDatabaseKey)

    fun count() = DatabaseAccess.executor().count(nodeDatabaseKey)

}