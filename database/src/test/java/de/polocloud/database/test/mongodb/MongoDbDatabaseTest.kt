package de.polocloud.database.test.mongodb

import de.polocloud.common.Address
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.database.nosql.mongo.MongoConnectionFactory
import de.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MongoDB")
class MongoDbDatabaseTest : GeneralDatabaseTest() {

    override fun factory(): DatabaseConnectionFactory<*> {
        return MongoConnectionFactory(
            DatabaseCredentials.MongoDB(
                Address(mongo.host, mongo.firstMappedPort),
                "test",
                "",
                "testdb"
            )
        )
    }

    companion object {
        @Container
        val mongo = MongoDBContainer("mongo:6.0.7")
    }
}