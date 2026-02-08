package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.node.cluster.Node
import dev.httpmarco.polocloud.node.database.DatabaseKey
import dev.httpmarco.polocloud.node.database.credentials.SqlDatabaseCredentials
import dev.httpmarco.polocloud.node.database.sql.SqlConnectionFactoryPart

object LocalNode {

    private val endpoint = GrpcEndpoint(5467)
    private val databaseConnectionFactory = SqlConnectionFactoryPart()

    init {
        endpoint.connect()

        // todo testing element -> replace with configuration
        this.databaseConnectionFactory.connect(SqlDatabaseCredentials("postgresql", "localhost", 5432, "postgres", "test123", "polo"))
    }
}