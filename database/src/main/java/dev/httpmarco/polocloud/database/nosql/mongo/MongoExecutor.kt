package dev.httpmarco.polocloud.database.nosql.mongo

import com.mongodb.client.MongoDatabase
import dev.httpmarco.polocloud.database.nosql.AbstractNoSqlExecutor
import org.bson.Document

class MongoExecutor(
    private val database: MongoDatabase
) : AbstractNoSqlExecutor() {

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
}
