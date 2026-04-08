package de.polocloud.node.repositories

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.nodes.NodeData
import java.util.UUID

object NodeRepository {

    private val nodeDatabaseKey = DatabaseKey(NodeData::class)

    fun find(id: UUID) = DatabaseAccess.executor().findById(nodeDatabaseKey, id)

    fun save(node: NodeData) = DatabaseAccess.executor().save(nodeDatabaseKey, node)

    fun findAll() = DatabaseAccess.executor().findAll(nodeDatabaseKey)

    fun count() = DatabaseAccess.executor().count(nodeDatabaseKey)

}