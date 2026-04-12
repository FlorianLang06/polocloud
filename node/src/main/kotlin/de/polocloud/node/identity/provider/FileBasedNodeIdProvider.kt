package de.polocloud.node.identity.provider

import de.polocloud.common.utils.toBytes
import de.polocloud.common.utils.toUUID
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class FileBasedNodeIdProvider : NodeIdProvider {

    override fun get(): UUID {
        val cachePath = Path(System.getProperty("rootDir"))
            .resolve(".cache")
            .resolve(".localId.dat")

        if (cachePath.exists()) {
            return Files.readAllBytes(cachePath).toUUID()
        }

        cachePath.parent.createDirectories()

        val uuid = UUID.randomUUID()
        Files.write(cachePath, uuid.toBytes())

        return uuid
    }
}