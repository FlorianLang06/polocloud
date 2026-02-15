package dev.httpmarco.polocloud.database.test.mongodb

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.junit.jupiter.api.DisplayName
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

@DisplayName("MongoDB")
class MongoDbDatabaseTest  : GeneralDatabaseTest() {
    override fun factory(): DatabaseConnectionFactory<*> {
        TODO("Not yet implemented")
    }

    override fun credentials(): DatabaseCredentials {
        TODO("Not yet implemented")
    }

    companion object {
        @Container
        val mongo = MongoDBContainer("mongo:6.0.7")
    }

}