package dev.httpmarco.polocloud.database.sql

import dev.httpmarco.polocloud.database.DatabaseExecutor
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.database.DatabaseState
import dev.httpmarco.polocloud.database.EntryIdentifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.sql.SQLException

class SqlExecutor(private val factory: SqlConnectionFactoryPart) : DatabaseExecutor {

    private val logger: Logger = LogManager.getLogger(SqlExecutor::class.java)

    override fun <T> save(key: DatabaseKey<T>, value: T) {
        if (factory.state != DatabaseState.CONNECTED) return

        ensureTableExists(key)

        val fields = key.clazz.declaredFields
        val identifierField = findIdentifierField(fields)
            ?: throw IllegalStateException("No @DatabaseIdentifier field found in ${key.clazz.simpleName}")


        identifierField.isAccessible = true
        val identifierValue = identifierField.get(value)

        val existsSql = "SELECT COUNT(*) FROM ${key.id} WHERE ${identifierField.name} = ?;"
        val exists = queryOne(
            existsSql,
            identifierValue,
            mapper = SqlMapper { rs -> rs.getInt(1) > 0 }
        ) ?: false

        val params = fields.map {
            it.isAccessible = true
            it.get(value)
        }

        if (exists) {
            val setClause = fields.joinToString(", ") { "${it.name} = ?" }
            val sql = "UPDATE ${key.id} SET $setClause WHERE ${identifierField.name} = ?;"
            update(sql, *(params + identifierValue).toTypedArray())
        } else {
            val columns = fields.joinToString(", ") { it.name }
            val placeholders = fields.joinToString(", ") { "?" }
            val sql = "INSERT INTO ${key.id} ($columns) VALUES ($placeholders);"
            update(sql, *params.toTypedArray())
        }
    }

    override fun <T> findAll(key: DatabaseKey<T>): List<T> {
        ensureTableExists(key)

        val sql = "SELECT * FROM ${key.id};"

        val clazz = key.clazz
        val constructor = clazz.declaredConstructors.first()
        constructor.isAccessible = true

        val fields = clazz.declaredFields

        return queryList(
            sql,
            mapper = SqlMapper { rs ->

                val args = fields.map { field ->
                    rs.getObject(field.name)
                }.toTypedArray()

                constructor.newInstance(*args) as T
            }
        )
    }


    override fun <T> exists(key: DatabaseKey<T>, value: T): Boolean {
        ensureTableExists(key)

        val identifierField = findIdentifierField(key.clazz.declaredFields)
            ?: throw IllegalStateException("No @DatabaseIdentifier field found in ${key.clazz.simpleName}")

        identifierField.isAccessible = true
        val identifierValue = identifierField.get(value)

        val sql = "SELECT COUNT(*) FROM ${key.id} WHERE ${identifierField.name} = ?;"

        return queryOne(
            sql,
            identifierValue,
            mapper = SqlMapper { rs -> rs.getInt(1) > 0 }
        ) ?: false
    }

    override fun <T> delete(key: DatabaseKey<T>, value: T) {
        ensureTableExists(key)

        val identifierField =
            key.clazz.declaredFields.find { it.getAnnotation(EntryIdentifier::class.java) != null }
                ?: throw IllegalStateException("No @DatabaseIdentifier field found")

        identifierField.isAccessible = true
        val sql = "DELETE FROM ${key.id} WHERE ${identifierField.name} = ?;"
        update(sql, identifierField.get(value))
    }

    override fun destroy(key: DatabaseKey<*>) {
        val sql = "DROP TABLE IF EXISTS ${key.id};"
        update(sql)
    }

    fun update(sql: String, vararg params: Any?): Int {
        if (!factory.isValid()) return -1

        val ds = factory.dataSource ?: throw IllegalStateException("DataSource not initialized")

        logger.debug("Executing update: $sql with params: ${params.joinToString()}")

        return try {
            ds.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.executeUpdate()
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed SQL update: $sql", ex)
            0
        }
    }

    fun <T> queryList(
        sql: String,
        vararg params: Any?,
        mapper: SqlMapper<T>
    ): List<T> {
        return executeQuery(sql, params, mapper)
    }

    fun <T> queryOne(
        sql: String,
        vararg params: Any?,
        mapper: SqlMapper<T>
    ): T? {
        return executeQuery(sql, params, mapper).firstOrNull()
    }

    private fun <T> executeQuery(
        sql: String,
        params: Array<out Any?>,
        mapper: SqlMapper<T>
    ): List<T> {

        if (!factory.isValid()) return emptyList()

        val ds = factory.dataSource ?: throw IllegalStateException("DataSource not initialized")
        val results = mutableListOf<T>()

        logger.debug("Executing query: $sql with params: ${params.joinToString()}")

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
            logger.error("Query failed: $sql", ex)
        }

        return results
    }

    private fun <T> ensureTableExists(key: DatabaseKey<T>) {
        val ds = factory.dataSource ?: throw IllegalStateException("DataSource not initialized")
        val table = key.id

        try {
            ds.connection.use { conn ->
                val meta = conn.metaData
                val rs = meta.getTables(null, null, table, arrayOf("TABLE"))

                if (!rs.next()) {
                    val fields = key.clazz.declaredFields

                    val columns = fields.joinToString(", ") { field ->
                        val sqlType = mapJavaTypeToSql(field.type)
                        val pk =
                            if (field.getAnnotation(EntryIdentifier::class.java) != null)
                                "PRIMARY KEY"
                            else ""

                        "${field.name} $sqlType $pk"
                    }

                    val sql = "CREATE TABLE $table ($columns);"
                    logger.debug("Creating table: $sql")
                    update(sql)
                }
            }
        } catch (ex: SQLException) {
            logger.error("Failed to ensure table exists: $table", ex)
        }
    }

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
