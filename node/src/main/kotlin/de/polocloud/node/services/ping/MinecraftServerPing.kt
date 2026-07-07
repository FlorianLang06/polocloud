package de.polocloud.node.services.ping

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Parsed result of a successful Minecraft Server List Ping (SLP).
 *
 * @param versionName   Version string the server reports (e.g. "Paper 1.20.4").
 * @param protocol      Protocol number the server speaks.
 * @param onlinePlayers Players currently connected.
 * @param maxPlayers    Configured player slots.
 * @param description   Flattened MOTD text (formatting codes stripped).
 * @param latencyMillis Round-trip time of the ping/pong exchange in milliseconds.
 */
data class MinecraftPingResult(
    val versionName: String,
    val protocol: Int,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val description: String,
    val latencyMillis: Long,
)

/**
 * Pings a Minecraft server (or proxy) over the modern Server List Ping protocol
 * (Minecraft 1.7+) and evaluates the JSON status response.
 *
 * The handshake advertises `next state = 1` (status), so the server answers with its
 * status JSON without a login being attempted. Both backend servers (Paper/Spigot)
 * and proxies (Velocity/Waterfall) reply to this on their listen port, which makes a
 * successful ping a reliable "the service is up and accepting connections" signal.
 *
 * All failures — connection refused, timeout, a half-started server that closes the
 * socket, malformed data — surface as `null` from [ping]; callers simply retry later.
 */
object MinecraftServerPing {

    private val json = Json { ignoreUnknownKeys = true }

    // The handshake carries the client's protocol version. -1 means "undetermined":
    // for a status ping the server echoes its own protocol regardless, so we never
    // need to match a specific game version here.
    private const val HANDSHAKE_PROTOCOL_UNDETERMINED = -1

    private const val PACKET_ID_HANDSHAKE = 0x00
    private const val PACKET_ID_STATUS_REQUEST = 0x00
    private const val PACKET_ID_PING = 0x01
    private const val NEXT_STATE_STATUS = 1

    /**
     * Pings [host]:[port] and returns the parsed status, or `null` if the server did
     * not answer a valid SLP response within [timeoutMillis] (per connect and read).
     */
    fun ping(host: String, port: Int, timeoutMillis: Int = 1500): MinecraftPingResult? =
        runCatching { pingOrThrow(host, port, timeoutMillis) }.getOrNull()

    private fun pingOrThrow(host: String, port: Int, timeoutMillis: Int): MinecraftPingResult {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
            socket.soTimeout = timeoutMillis

            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Handshake: protocol version, address, port, next state = status.
            val handshake = ByteArrayOutputStream()
            DataOutputStream(handshake).apply {
                writeVarInt(this, PACKET_ID_HANDSHAKE)
                writeVarInt(this, HANDSHAKE_PROTOCOL_UNDETERMINED)
                writeString(this, host)
                writeShort(port)
                writeVarInt(this, NEXT_STATE_STATUS)
            }
            writePacket(out, handshake.toByteArray())

            // Empty status request triggers the JSON status response.
            val request = ByteArrayOutputStream()
            writeVarInt(DataOutputStream(request), PACKET_ID_STATUS_REQUEST)
            writePacket(out, request.toByteArray())

            // Status response: length | packetId(0x00) | jsonLength | json.
            readVarInt(input) // total packet length — not needed, framing is self-describing
            val packetId = readVarInt(input)
            require(packetId == PACKET_ID_STATUS_REQUEST) {
                "Unexpected packet id $packetId in status response"
            }
            val payload = ByteArray(readVarInt(input))
            input.readFully(payload)

            // Ping/pong measures latency. Best-effort: some proxies close right after
            // the status, so a missing pong must not fail an otherwise valid ping.
            val start = System.currentTimeMillis()
            val pingPacket = ByteArrayOutputStream()
            DataOutputStream(pingPacket).apply {
                writeVarInt(this, PACKET_ID_PING)
                writeLong(start)
            }
            writePacket(out, pingPacket.toByteArray())
            val latency = runCatching {
                readVarInt(input) // length
                readVarInt(input) // packet id (0x01)
                input.readLong()  // echoed payload
                System.currentTimeMillis() - start
            }.getOrDefault(System.currentTimeMillis() - start)

            return parse(String(payload, Charsets.UTF_8), latency)
        }
    }

    private fun parse(raw: String, latency: Long): MinecraftPingResult {
        val root = json.parseToJsonElement(raw).jsonObject
        val version = root["version"]?.jsonObject
        val players = root["players"]?.jsonObject

        return MinecraftPingResult(
            versionName = version?.get("name")?.jsonPrimitive?.contentOrNull ?: "unknown",
            protocol = version?.get("protocol")?.jsonPrimitive?.intOrNull ?: -1,
            onlinePlayers = players?.get("online")?.jsonPrimitive?.intOrNull ?: 0,
            maxPlayers = players?.get("max")?.jsonPrimitive?.intOrNull ?: 0,
            description = flattenDescription(root["description"]),
            latencyMillis = latency,
        )
    }

    /**
     * Flattens the MOTD, which may be a plain string or a chat component object with a
     * `text` field and nested `extra` parts. Formatting is dropped — only the concatenated
     * text is kept.
     */
    private fun flattenDescription(element: JsonElement?): String = when (element) {
        null -> ""
        is JsonPrimitive -> element.contentOrNull ?: ""
        is JsonObject -> {
            val text = element["text"]?.jsonPrimitive?.contentOrNull ?: ""
            val extra = (element["extra"] as? kotlinx.serialization.json.JsonArray)
                ?.joinToString("") { flattenDescription(it) }
                ?: ""
            (text + extra).trim()
        }
        else -> ""
    }

    /** Prefixes [data] with its VarInt length and writes it as one framed packet. */
    private fun writePacket(out: DataOutputStream, data: ByteArray) {
        writeVarInt(out, data.size)
        out.write(data)
        out.flush()
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var remaining = value
        while (true) {
            if (remaining and 0x7F.inv() == 0) {
                out.writeByte(remaining)
                return
            }
            out.writeByte((remaining and 0x7F) or 0x80)
            remaining = remaining ushr 7
        }
    }

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var position = 0
        while (true) {
            val current = input.readByte().toInt()
            result = result or ((current and 0x7F) shl (position * 7))
            if (current and 0x80 == 0) return result
            position++
            require(position < 5) { "VarInt is too big" }
        }
    }
}