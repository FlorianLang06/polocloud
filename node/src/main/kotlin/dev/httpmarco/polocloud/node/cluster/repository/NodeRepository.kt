package dev.httpmarco.polocloud.node.cluster.repository

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.node.cluster.node.data.NodeData
import java.util.UUID

class NodeRepository(val database: DatabaseConnectionFactory<*>) {

    private val repositoryKey = DatabaseKey(NodeData::class)

    fun findNode(identifier: UUID): NodeData? = this.database.executor().findById(repositoryKey, identifier);

    fun save(node: NodeData) = this.database.executor().save(repositoryKey, node)

}