package de.polocloud.node.nodes

import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import java.util.UUID

class NodeRepository(private val database: DatabaseConnectionFactory<*>) {

    private val nodeDatabaseKey = DatabaseKey(NodeData::class)

    fun find(id: UUID) = this.database.executor().findById(nodeDatabaseKey, id)

    fun save(node: NodeData) = this.database.executor().save(nodeDatabaseKey, node)

    fun findAll() = this.database.executor().findAll(nodeDatabaseKey)

    fun count() = this.database.executor().count(nodeDatabaseKey)

}