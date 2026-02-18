package dev.httpmarco.polocloud.database.nosql.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.DatabaseState
import dev.httpmarco.polocloud.i18n.api.TranslationService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class MongoConnectionFactoryPart(credentials: DatabaseCredentials.MongoDB) : DatabaseConnectionFactory<DatabaseCredentials.MongoDB>(credentials) {

    companion object {
        private val logger: Logger = LogManager.getLogger(MongoConnectionFactoryPart::class.java)
    }

    private var executor : MongoExecutor? = null;

    var client: MongoClient? = null

    override fun connect(credentials: DatabaseCredentials.MongoDB) {
        state = DatabaseState.CONNECTING
        logger.info(TranslationService.tr("database", "database.connection.connecting"))

        try {

            val connectionString = if (credentials.password != null) {
                "mongodb://${credentials.username}:${credentials.password}@${credentials.address.asString()}/${credentials.database}"
            } else {
                "mongodb://${credentials.address.asString()}/${credentials.database}"
            }

            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .build()

            client = MongoClients.create(settings)
            this.executor = MongoExecutor(client!!.getDatabase(credentials.database))

            state = DatabaseState.CONNECTED

            logger.info(
                TranslationService.tr(
                    "database",
                    "database.connection.established",
                    "driver" to "mongodb"
                )
            )

        } catch (e: Exception) {
            state = DatabaseState.FAILED
            logger.error(TranslationService.tr("database", "database.connection.failed"), e)
        }
    }

    override fun executor() = executor!!

    override fun close() {
        try {
            client?.close()
            client = null

            state = DatabaseState.CLOSED

            logger.info(
                TranslationService.tr(
                    "database",
                    "database.connection.closed"
                )
            )

        } catch (e: Exception) {
            logger.error("MongoDB close failed", e)
        }
    }
}
