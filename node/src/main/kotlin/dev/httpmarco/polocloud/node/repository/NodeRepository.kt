package dev.httpmarco.polocloud.node.repository

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import java.util.UUID

class NodeRepository(val database: DatabaseConnectionFactory<*>) {

    private val repositoryKey = DatabaseKey(_root_ide_package_.dev.httpmarco.polocloud.node.node.data.NodeData::class)

    fun findNode(identifier: UUID): dev.httpmarco.polocloud.node.node.data.NodeData? = this.database.executor().findById(repositoryKey, identifier);

    fun save(node: dev.httpmarco.polocloud.node.node.data.NodeData) = this.database.executor().save(repositoryKey, node)

    fun findAll() = this.database.executor().findAll(repositoryKey)

}