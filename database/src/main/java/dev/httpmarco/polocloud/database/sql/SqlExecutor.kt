package dev.httpmarco.polocloud.database.sql

import dev.httpmarco.polocloud.database.DatabaseExecutor
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.database.DatabaseState
import dev.httpmarco.polocloud.database.EntryIdentifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.sql.SQLException

/**
 * SQL executor implementation using HikariCP for database connections.
 * Provides generic query execution and mapping to Kotlin objects, as well as
 * insert, update, delete, and destroy operations.
 *
 * Automatically creates tables if they do not exist, using the @DatabaseIdentifier
 * field as PRIMARY KEY.
 *
 * @property factory The connection factory providing the HikariDataSource.
 */
class SqlExecutor(private val factory: SqlConnectionFactoryPart) : DatabaseExecutor {

    private val logger: Logger = LogManager.getLogger(SqlExecutor::class.java)

    /**
     * Retrieves all rows from the table associated with the given DatabaseKey.
     */
    override fun <T> findAll(key: DatabaseKey<T>): List<T> {
        ensureTableExists(key)
        val sql = "SELECT * FROM ${key.id};"
        logger.debug("Executing findAll query: $sql")

        return query(sql, mapper = SqlMapper<T> { set ->
            val instance = key.clazz.getDeclaredConstructor().newInstance()
            key.clazz.declaredFields.forEach { field ->
                field.isAccessible = true
                field.set(instance, set.getObject(field.name))
            }
            instance
        })
    }

    /**
     * Inserts a new row into the table associated with the given DatabaseKey.
     */
    override fun <T> insert(key: DatabaseKey<T>, value: T) {
        ensureTableExists(key)

        val fields = key.clazz.declaredFields
        val columns = fields.joinToString(", ") { it.name }
        val placeholders = fields.joinToString(", ") { "?" }
        val sql = "INSERT INTO ${key.id} ($columns) VALUES ($placeholders);"

        val params = fields.map { field ->
            field.isAccessible = true
            field.get(value)
        }

        update(sql, *params.toTypedArray())
    }

    /**
     * Updates an existing row in the table associated with the given DatabaseKey.
     */
    override fun <T> update(key: DatabaseKey<T>, value: T) {
        if (factory.state != DatabaseState.CONNECTED) {
            return
        }

        ensureTableExists(key)

        val fields = key.clazz.declaredFields
        val identifierField =
            fields.find { it.getAnnotation(EntryIdentifier::class.java) != null }
                ?: throw IllegalStateException("No @DatabaseIdentifier field found in ${key.clazz.simpleName}")

        val setClause = fields.joinToString(", ") { "${it.name} = ?" }
        val sql = "UPDATE ${key.id} SET $setClause WHERE ${identifierField.name} = ?;"

        val params = fields.map { field ->
            field.isAccessible = true
            field.get(value)
        } + listOf(identifierField.get(value))

        update(sql, *params.toTypedArray())
    }

    /**
     * Deletes a row from the table associated with the given DatabaseKey.
     */
    override fun <T> delete(key: DatabaseKey<T>, value: T) {
        ensureTableExists(key)

        val identifierField =
            key.clazz.declaredFields.find { it.getAnnotation(EntryIdentifier::class.java) != null }
                ?: throw IllegalStateException("No @DatabaseIdentifier field found in ${key.clazz.simpleName}")

        identifierField.isAccessible = true
        val sql = "DELETE FROM ${key.id} WHERE ${identifierField.name} = ?;"
        update(sql, identifierField.get(value))
    }

    /**
     * Destroys resources associated with a specific DatabaseKey (drops the table).
     */
    override fun destroy(key: DatabaseKey<*>) {
        val sql = "DROP TABLE IF EXISTS ${key.id};"
        update(sql)
    }

    /**
     * Executes a generic SQL update statement (INSERT/UPDATE/DELETE) with optional parameters.
     */
    fun update(sql: String, vararg params: Any?): Int {
        if (!factory.isValid()) {
            return -1
        }

        val ds = factory.dataSource ?: throw IllegalStateException("DataSource is not initialized")
        logger.debug("Executing SQL update: $sql with params: ${params.joinToString()}")

        return try {
            ds.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    val affected = stmt.executeUpdate()
                    logger.debug("SQL update affected $affected row(s)")
                    affected
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to execute SQL update: $sql", ex)
            0
        }
    }

    /**
     * Executes a SQL query with optional parameters and maps each result row using the provided mapper.
     */
    fun <T> query(sql: String, vararg params: Any?, mapper: SqlMapper<T>): List<T> {

        if (!factory.isValid()) {
            return emptyList()
        }

        val ds = factory.dataSource ?: throw IllegalStateException("DataSource is not initialized")
        val results = arrayListOf<T>()

        logger.debug("Executing SQL: $sql with params: ${params.joinToString()}")

        try {
            ds.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }

                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(mapper.map(rs))
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Error executing query: $sql", ex)
        }

        logger.debug("Query executed successfully, returned ${results.size} rows")
        return results
    }

    /**
     * Ensures that a table for the given DatabaseKey exists.
     * If it does not exist, it will be created with all fields as columns.
     * The field annotated with @DatabaseIdentifier is used as PRIMARY KEY.
     */
    private fun <T> ensureTableExists(key: DatabaseKey<T>) {
        val ds = factory.dataSource ?: throw IllegalStateException("DataSource is not initialized")
        val table = key.id

        try {
            ds.connection.use { conn ->
                // Check if table exists
                val meta = conn.metaData
                val rs = meta.getTables(null, null, table, arrayOf("TABLE"))
                if (!rs.next()) {
                    // Table does not exist, create it
                    val fields = key.clazz.declaredFields
                    val columns = fields.joinToString(", ") { field ->
                        val sqlType = mapJavaTypeToSql(field.type)
                        val pk =
                            if (field.getAnnotation(EntryIdentifier::class.java) != null) "PRIMARY KEY" else ""
                        "${field.name} $sqlType $pk"
                    }
                    val sql = "CREATE TABLE $table ($columns);"
                    logger.debug("Creating table $table: $sql")
                    update(sql)
                }
            }
        } catch (ex: SQLException) {
            logger.error("Failed to ensure table $table exists", ex)
        }
    }

    /**
     * Maps a Java/Kotlin type to a corresponding SQL column type.
     */
    private fun mapJavaTypeToSql(clazz: Class<*>): String =
        when (clazz.kotlin) {
            Int::class -> "INT"
            Long::class -> "BIGINT"
            String::class -> "VARCHAR(255)"
            Boolean::class -> "BOOLEAN"
            Double::class -> "DOUBLE"
            Float::class -> "FLOAT"
            else -> "TEXT"
        }
}
