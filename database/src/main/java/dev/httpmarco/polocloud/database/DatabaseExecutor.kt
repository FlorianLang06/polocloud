package dev.httpmarco.polocloud.database

/**
 * A generic executor interface for performing CRUD operations on database tables.
 *
 * Implementations should provide mechanisms for querying, updating, deleting,
 * and destroying database tables based on a [dev.httpmarco.polocloud.database.DatabaseKey].
 */
interface DatabaseExecutor {

    /**
     * Inserts a new row or update into the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table and target type.
     * @param value The object to be inserted as a new row in the database.
     */
    fun <T> save(key: DatabaseKey<T>, value: T)

    /**
     * Retrieves all rows from the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table and target type.
     * @return A list of objects of type [T] mapped from the database rows.
     */
    fun <T> findAll(key: DatabaseKey<T>): List<T>

    /**
     * Deletes a row from the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * The row to delete is identified using the field annotated with
     * [EntryIdentifier] in the object [value].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table.
     * @param value The object representing the row to delete.
     */
    fun <T> delete(key: DatabaseKey<T>, value: T)

    /**
     * Destroys the resources associated with a specific [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * Typically, this involves dropping the table from the database.
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] whose resources should be destroyed.
     */
    fun destroy(key: DatabaseKey<*>)

    /**
     * Checks if a row exists in the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * The existence check is typically performed using the field annotated with [EntryIdentifier] in the object
     * represented by the [DatabaseKey].
     *
     * @return true if a matching row exists, false otherwise.
     */
    fun <T> exists(key: DatabaseKey<T>, value: T) : Boolean

    /**
     * Finds the field in the given array of fields that is annotated with [EntryIdentifier].
     */
    fun findIdentifierField(fields: Array<java.lang.reflect.Field>): java.lang.reflect.Field? {
        return fields.find { field ->
            field.getAnnotation(EntryIdentifier::class.java) != null ||
                    field.annotations.any { it.annotationClass.java == EntryIdentifier::class.java }
        }
    }
}