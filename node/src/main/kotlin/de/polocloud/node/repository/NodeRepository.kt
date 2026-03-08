package de.polocloud.node.repository

import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.node.data.NodeData
import java.util.UUID

class NodeRepository(val database: DatabaseConnectionFactory<*>) {

    private val repositoryKey = DatabaseKey(NodeData::class)

    fun findNode(identifier: UUID): NodeData? = this.database.executor().findById(repositoryKey, identifier);

    fun save(node: NodeData) = this.database.executor().save(repositoryKey, node)

    fun findAll() = this.database.executor().findAll(repositoryKey)

}