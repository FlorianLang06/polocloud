package de.polocloud.node.services.factory.platform.custom

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey

object CustomPlatformRepository {

    private val customPlatformDatabaseKey = DatabaseKey(CustomPlatform::class)

    fun find(name: String) = DatabaseAccess.executor().findById(customPlatformDatabaseKey, name)

    fun save(platform: CustomPlatform) = DatabaseAccess.executor().save(customPlatformDatabaseKey, platform)

    fun delete(platform: CustomPlatform) = DatabaseAccess.executor().delete(customPlatformDatabaseKey, platform)

    fun findAll() = DatabaseAccess.executor().findAll(customPlatformDatabaseKey)

    fun exists(name: String) = DatabaseAccess.executor().findById(customPlatformDatabaseKey, name) != null
}
