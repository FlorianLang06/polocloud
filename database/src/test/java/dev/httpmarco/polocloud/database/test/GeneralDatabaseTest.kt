package dev.httpmarco.polocloud.database.test

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

abstract class GeneralDatabaseTest {

    private val key = DatabaseKey("testdb", TestObject::class.java)
    private val factory by lazy { factory() }

    init {
        TranslationService.init()
    }

    @BeforeEach
    fun connect() {
        factory.globalConnect(credentials())
        assert(factory.isValid())
    }

    @Test
    fun insert() {

    }

    @Test
    fun findAll() {
        val objects = factory.executor().findAll(key)

        assertNotNull(objects)
        assert(objects.count() == 0)
    }

    @Test
    fun findById() {

    }

    @Test
    fun save() {

    }

    @Test
    fun exists() {

    }

    @Test
    fun delete() {

    }

    abstract fun factory() : DatabaseConnectionFactory<*>

    abstract fun credentials() : DatabaseCredentials
}