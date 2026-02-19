package dev.httpmarco.polocloud.database.nosql.mongo

import com.mongodb.client.MongoDatabase
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.database.DatabaseSerializer
import dev.httpmarco.polocloud.database.filtering.Filter
import dev.httpmarco.polocloud.database.filtering.FilterTranslator
import dev.httpmarco.polocloud.database.nosql.AbstractNoSqlExecutor
import org.bson.Document

class MongoExecutor(
    private val database: MongoDatabase
) : AbstractNoSqlExecutor() {

    private val filterTranslator = MongoFilterTranslator()

    override fun write(collection: String, identifier: String, json: String) {
        val col = database.getCollection(collection)

        val doc = Document.parse(json)
        doc["_id"] = identifier

        col.replaceOne(
            Document("_id", identifier),
            doc,
            com.mongodb.client.model.ReplaceOptions().upsert(true)
        )
    }

    override fun readAll(collection: String): List<String> {

        val col = database.getCollection(collection)

        return col.find().map { it.toJson() }.toList()
    }

    override fun deleteInternal(collection: String, identifier: String) {
        database.getCollection(collection)
            .deleteOne(Document("_id", identifier))
    }

    override fun existsInternal(collection: String, identifier: String): Boolean {
        return database.getCollection(collection)
            .find(Document("_id", identifier))
            .first() != null
    }

    override fun destroyInternal(collection: String) {
        database.getCollection(collection).drop()
    }

    override fun <T : Any> findById(key: DatabaseKey<T>, id: Any): T? {
        val collection = database.getCollection(key.id())

        val document = collection
            .find(Document("_id", id.toString()))
            .first() ?: return null

        return DatabaseSerializer.deserialize(
            document.toJson(),
            key.clazz
        )
    }

    override fun <T : Any> find(
        key: DatabaseKey<T>,
        vararg filters: Filter
    ): List<T> {
        TODO("Not yet implemented")
    }

    override fun filterTranslator() = filterTranslator

}
