package dev.httpmarco.polocloud.database

/**
 * A generic executor interface for performing CRUD operations on database tables.
 *
 * Implementations should provide mechanisms for querying, updating, deleting,
 * and destroying database tables based on a [dev.httpmarco.polocloud.database.DatabaseKey].
 */
interface DatabaseExecutor {

    /**
     * Inserts a new row into the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table and target type.
     * @param value The object to be inserted as a new row in the database.
     */
    fun <T> insert(key: DatabaseKey<T>, value: T)

    /**
     * Retrieves all rows from the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table and target type.
     * @return A list of objects of type [T] mapped from the database rows.
     */
    fun <T> findAll(key: DatabaseKey<T>): List<T>

    /**
     * Updates an existing row in the table associated with the given [dev.httpmarco.polocloud.database.DatabaseKey].
     *
     * The row to update is typically identified using a field annotated with
     * [EntryIdentifier] in the object [value].
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] representing the table.
     * @param value The object containing updated values.
     */
    fun <T> update(key: DatabaseKey<T>, value: T)

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
     * Typically this involves dropping the table from the database.
     *
     * @param key The [dev.httpmarco.polocloud.database.DatabaseKey] whose resources should be destroyed.
     */
    fun destroy(key: DatabaseKey<*>)
}