package dev.httpmarco.polocloud.database.test

import dev.httpmarco.polocloud.database.DatabaseKey
import org.junit.Test

abstract class GeneralDatabaseTest {

    private val key = DatabaseKey("testdb", TestObject::class.java)

    @Test
    fun findAll() {

    }

}