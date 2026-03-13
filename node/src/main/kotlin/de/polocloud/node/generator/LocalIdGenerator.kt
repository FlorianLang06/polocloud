package de.polocloud.node.generator

import de.polocloud.common.utils.toBytes
import de.polocloud.common.utils.toUUID
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.createDirectories

object LocalIdGenerator : Generator<UUID> {

    private val cachePath = Path(".cache").resolve(".localId.dat")

    override fun generate(): UUID {
        if (cachePath.exists()) {
            val bytes = Files.readAllBytes(cachePath)
            return bytes.toUUID()
        } else {
            cachePath.parent.createDirectories()

            val uuid = UUID.randomUUID()
            Files.write(cachePath, uuid.toBytes())
            return uuid
        }
    }
}