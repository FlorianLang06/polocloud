package dev.httpmarco.polocloud.node.storage.database.credentials

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import dev.httpmarco.polocloud.node.storage.database.DatabaseType
import java.lang.reflect.Type

class DatabaseCredentialsConfigurationAdapter : JsonSerializer<DatabaseCredentials>,
    JsonDeserializer<DatabaseCredentials> {

    override fun serialize(
        credentials: DatabaseCredentials,
        p1: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        val data = context.serialize(credentials).asJsonObject
        data.addProperty("type", credentials.type().name)
        return data
    }

    override fun deserialize(
        element: JsonElement,
        p1: Type?,
        p2: JsonDeserializationContext
    ): DatabaseCredentials? {
        val data = element.asJsonObject
        val type = DatabaseType.valueOf(data.get("type").asString)

        if (type == DatabaseType.SQL) {
            return p2.deserialize(data, SqlDatabaseCredentials::class.java)
        } else if (type == DatabaseType.NOSQL) {
            return p2.deserialize(data, NoSqlDatabaseCredentials::class.java)
        }
        throw IllegalArgumentException("Unsupported database type: $type")
    }
}