package dev.httpmarco.polocloud.database.test.mongodb

import dev.httpmarco.polocloud.database.test.GeneralDatabaseTest
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

class MongoDbDatabaseTest  : GeneralDatabaseTest() {

    companion object {
        @Container
        val mongo = MongoDBContainer("mongo:6.0.7")
    }

}