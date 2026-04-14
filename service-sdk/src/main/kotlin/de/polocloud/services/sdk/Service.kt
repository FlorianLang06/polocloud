package de.polocloud.services.sdk

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseExecutor

abstract class Service {

    private val databaseExecutor : DatabaseExecutor = DatabaseAccess.executor()

    fun database() = databaseExecutor

}