package de.polocloud.node.bootstrap

import de.polocloud.common.generator.Generator
import de.polocloud.common.utils.toBytes
import de.polocloud.common.utils.toUUID
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

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