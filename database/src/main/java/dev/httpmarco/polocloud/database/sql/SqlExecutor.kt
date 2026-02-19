package dev.httpmarco.polocloud.database.sql

import dev.httpmarco.polocloud.database.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.sql.Timestamp
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant
import kotlin.time.ExperimentalTime


/**
 * SQL-based implementation of [DatabaseExecutor].
 *
 * This executor provides automatic table creation and reflection-based
 * entity mapping. The field annotated with [EntryIdentifier] is used
 * as the primary key.
 *
 * Features:
 * - Automatic table generation
 * - Insert or update (upsert-like behavior)
 * - Enum stored as String
 * - UUID stored as String
 *
 * Note:
 * This implementation prioritizes simplicity over reflection caching.
 */
class SqlExecutor(
    private val factory: SqlConnectionFactory
) : DatabaseExecutor {

    private val logger: Logger = LogManager.getLogger(SqlExecutor::class.java)

    /**
     * Saves or updates an entity in the database.
     *
     * If an entity with the same primary key already exists,
     * an UPDATE is executed. Otherwise, an INSERT is performed.
     *
     * @param key   the database key describing table and entity type
     * @param value the entity instance to persist
     *
     * @throws IllegalStateException if no [EntryIdentifier] field exists
     */
    override fun <T : Any> save(key: DatabaseKey<T>, value: T) {
        if (!factory.isValid()) return

        ensureTableExists(key)
        val meta = resolveMeta(key)

        val idValue = meta.identifier.get(value)
        val exists = findById(key, idValue) != null

        val values = meta.fields.map { field ->
            val raw = field.get(value)
            if (field.type.isEnum) (raw as Enum<*>).name else raw
        }

        if (exists) {
            val setClause = meta.fields.joinToString(", ") { "${it.name} = ?" }
            val sql = "UPDATE ${key.id} SET $setClause WHERE ${meta.identifier.name} = ?"
            update(sql, *(values + idValue).toTypedArray())
        } else {
            val columns = meta.fields.joinToString(", ") { it.name }
            val placeholders = meta.fields.joinToString(", ") { "?" }
            val sql = "INSERT INTO ${key.id} ($columns) VALUES ($placeholders)"
            update(sql, *values.toTypedArray())
        }
    }

    /**
     * Retrieves all entities from the table.
     *
     * @param key the database key
     * @return list of mapped entities
     */
    override fun <T : Any> findAll(key: DatabaseKey<T>): List<T> {
        ensureTableExists(key)
        val meta = resolveMeta(key)

        return queryList(
            "SELECT * FROM ${key.id}",
            mapper = SqlMapper { rs -> mapRow(meta, rs) }
        )
    }

    /**
     * Retrieves a single entity by its primary key.
     *
     * @param key the database key
     * @param id  primary key value
     * @return mapped entity or null if not found
     */
    override fun <T : Any> findById(key: DatabaseKey<T>, id: Any): T? {
        ensureTableExists(key)
        val meta = resolveMeta(key)

        return queryOne(
            "SELECT * FROM ${key.id} WHERE ${meta.identifier.name} = ? LIMIT 1",
            id,
            mapper = SqlMapper { rs -> mapRow(meta, rs) }
        )
    }

    /**
     * Checks whether an entity exists in the database.
     *
     * @param key   the database key
     * @param value entity instance
     * @return true if present
     */
    override fun <T : Any> exists(key: DatabaseKey<T>, value: T): Boolean {
        val meta = resolveMeta(key)
        return findById(key, meta.identifier.get(value)) != null
    }

    /**
     * Deletes an entity from the database.
     *
     * @param key   the database key
     * @param value entity instance to delete
     */
    override fun <T : Any> delete(key: DatabaseKey<T>, value: T) {
        ensureTableExists(key)
        val meta = resolveMeta(key)

        val idValue = meta.identifier.get(value)
        update("DELETE FROM ${key.id} WHERE ${meta.identifier.name} = ?", idValue)
    }

    /**
     * Drops the table represented by the given key.
     *
     * @param key the database key
     */
    override fun destroy(key: DatabaseKey<*>) {
        update("DROP TABLE IF EXISTS ${key.id}")
    }

    private data class EntityMeta<T>(
        val fields: List<Field>,
        val identifier: Field,
        val constructor: Constructor<*>
    )

    private fun <T : Any> resolveMeta(key: DatabaseKey<T>): EntityMeta<T> {
        val clazz = key.clazz.java // important!

        val fields = clazz.declaredFields.toList().onEach { it.isAccessible = true }

        val identifier = fields.find {
            it.getAnnotation(EntryIdentifier::class.java) != null
        } ?: throw IllegalStateException(
            "No @EntryIdentifier field found in ${clazz.simpleName}"
        )

        val constructor = clazz.declaredConstructors.first().apply { isAccessible = true }

        return EntityMeta<T>(fields, identifier, constructor)
    }


    /**
     * Executes an SQL update statement.
     *
     * @param sql    SQL string
     * @param params statement parameters
     * @return affected row count or -1 if invalid
     */
    fun update(sql: String, vararg params: Any?): Int {
        if (!factory.isValid()) return -1

        val ds = factory.dataSource
            ?: throw IllegalStateException("DataSource not initialized")

        return try {
            ds.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { i, p ->
                        stmt.setObject(i + 1, mapValueForDb(p)) // <-- hier
                    }
                    stmt.executeUpdate()
                }
            }
        } catch (ex: Exception) {
            logger.error("SQL update failed: $sql", ex)
            0
        }
    }

    /**
     * Executes an SQL query returning multiple results.
     */
    fun <T> queryList(
        sql: String,
        vararg params: Any?,
        mapper: SqlMapper<T>
    ): List<T> = executeQuery(sql, params, mapper)

    /**
     * Executes an SQL query returning a single result.
     */
    fun <T> queryOne(
        sql: String,
        vararg params: Any?,
        mapper: SqlMapper<T>
    ): T? = executeQuery(sql, params, mapper).firstOrNull()

    private fun <T> executeQuery(
        sql: String,
        params: Array<out Any?>,
        mapper: SqlMapper<T>
    ): List<T> {

        if (!factory.isValid()) return emptyList()

        val ds = factory.dataSource
            ?: throw IllegalStateException("DataSource not initialized")

        val results = mutableListOf<T>()

        try {
            ds.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->

                    params.forEachIndexed { i, p ->
                        stmt.setObject(i + 1, p)
                    }

                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(mapper.map(rs))
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("SQL query failed: $sql", ex)
        }

        return results
    }

    /**
     * Ensures the SQL table for the given entity exists.
     * If not, it will be created dynamically.
     */
    private fun <T : Any> ensureTableExists(key: DatabaseKey<T>) {
        val ds = factory.dataSource
            ?: throw IllegalStateException("DataSource not initialized")

        try {
            ds.connection.use { conn ->
                val meta = conn.metaData
                val rs = meta.getTables(null, null, key.id, arrayOf("TABLE"))

                if (!rs.next()) {
                    val metaInfo = resolveMeta(key)

                    val columns = metaInfo.fields.joinToString(", ") { field ->
                        val type = mapJavaTypeToSql(field.type)
                        val pk = if (field == metaInfo.identifier) "PRIMARY KEY" else ""
                        "${field.name} $type $pk"
                    }

                    update("CREATE TABLE IF NOT EXISTS ${key.id} ($columns)")
                }
            }
        } catch (ex: SQLException) {
            logger.error("Failed to ensure table exists: ${key.id}", ex)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun mapValueForDb(value: Any?): Any? {
        return when (value) {
            is KotlinInstant -> Timestamp.from(JavaInstant.ofEpochMilli(value.toEpochMilliseconds()))
            is Enum<*> -> value.name
            is UUID -> value.toString()
            else -> value
        }
    }

    private fun <T> mapRow(meta: EntityMeta<T>, rs: ResultSet): T {
        val args = meta.fields.map { field ->
            val value = rs.getObject(field.name)

            when {
                field.type.isEnum && value is String ->
                    java.lang.Enum.valueOf(field.type as Class<out Enum<*>>, value)

                field.type == UUID::class.java && value is String ->
                    UUID.fromString(value)

                field.type.kotlin == KotlinInstant::class -> when (value) {
                    is Timestamp -> KotlinInstant.fromEpochMilliseconds(value.time)
                    is String -> KotlinInstant.parse(value)  // <- String → Instant
                    is JavaInstant -> KotlinInstant.fromEpochMilliseconds(value.toEpochMilli())
                    else -> throw IllegalArgumentException("Cannot convert $value to KotlinInstant")
                }

                else -> value
            }
        }.toTypedArray()

        return meta.constructor.newInstance(*args) as T
    }



    private fun mapJavaTypeToSql(clazz: Class<*>): String =
        when {
            clazz.isEnum -> "VARCHAR(50)"
            clazz.kotlin == Int::class -> "INT"
            clazz.kotlin == Long::class -> "BIGINT"
            clazz.kotlin == String::class -> "VARCHAR(512)"
            clazz.kotlin == Boolean::class -> "BOOLEAN"
            clazz.kotlin == Double::class -> "DOUBLE"
            clazz.kotlin == Float::class -> "FLOAT"
            clazz.kotlin == KotlinInstant::class -> "TIMESTAMP"
            clazz == UUID::class.java -> "VARCHAR(36)"
            else -> "TEXT"
        }

    override fun findIdentifierField(fields: Array<Field>): Field? =
        fields.find { it.getAnnotation(EntryIdentifier::class.java) != null }
}
