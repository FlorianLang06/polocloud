package de.polocloud.node.generator

import de.polocloud.common.utils.toBytes
import de.polocloud.common.utils.toUUID
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.createDirectories

object LocalIdGenerator : Generator<UUID> {

    override fun generate(): UUID {
        // at generate we need to check dir
        val cachePath = Path(System.getProperty("rootDir")).resolve(".cache").resolve(".localId.dat")

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