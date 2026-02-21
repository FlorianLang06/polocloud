package dev.httpmarco.polocloud.database.test

import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class GeneralDatabaseTest {

    private val log = LoggerFactory.getLogger(GeneralDatabaseTest::class.java)

    private lateinit var factory: DatabaseConnectionFactory<*>
    private lateinit var key: DatabaseKey<TestObject>

    private val dummyValue = TestObject("dummy", 42)

    init {
        TranslationService.init()
    }

    @BeforeEach
    fun setup() {
        factory = factory()
        factory.connect(credentials())

        assertTrue(factory.isValid())

        key = DatabaseKey("testdb", TestObject::class.java)

        // ensure clean state
        factory.executor().destroy(key)
    }

    @AfterEach
    fun cleanup() {
        factory.executor().destroy(key)
        factory.close()
    }

    @Test
    fun `findAll should return empty list when table is empty`() {
        var objects = factory.executor().findAll(key)

        log.info("Found ${objects.size} objects")

        assertTrue(objects.isEmpty())

        factory.executor().save(key, dummyValue)
        objects = factory.executor().findAll(key)

        log.info("Found ${objects.size} objects")
        assertTrue(!objects.isEmpty())
    }

    @Test
    fun `save should insert object`() {
        factory.executor().save(key, dummyValue)

        val objects = factory.executor().findAll(key)

        assertEquals(1, objects.size)
        assertEquals(dummyValue.name, objects.first().name)
    }

    @Test
    fun `exists should return true after insert`() {
        factory.executor().save(key, dummyValue)

        val result = factory.executor().exists(key, dummyValue)

        assertTrue(result)
    }

    @Test
    fun `exists should return false when not present`() {
        val result = factory.executor().exists(key, dummyValue)

        assertFalse(result)
    }


    @Test
    fun `destroy should remove table data`() {
        factory.executor().save(key, dummyValue)

        factory.executor().destroy(key)

        val objects = factory.executor().findAll(key)

        assertTrue(objects.isEmpty())
    }

    abstract fun factory(): DatabaseConnectionFactory<*>
}
