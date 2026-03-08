package de.polocloud.common.utils

import java.nio.ByteBuffer
import java.util.UUID

// Extension für UUID → ByteArray
fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).apply {
    putLong(mostSignificantBits)
    putLong(leastSignificantBits)
}.array()

// Extension für ByteArray → UUID
fun ByteArray.toUUID(): UUID {
    require(size == 16) { "Invalid byte array size for UUID: $size" }
    val buffer = ByteBuffer.wrap(this)
    return UUID(buffer.long, buffer.long)
}
