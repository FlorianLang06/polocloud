package dev.httpmarco.polocloud.common.utils

import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URI

/**
 * Returns the local IPv4 address of the device.
 *
 * Iterates through all network interfaces and returns the first non-loopback IPv4 address found.
 *
 * @return Local IPv4 address as [String]
 * @throws IllegalArgumentException if no suitable local IP is found
 */
fun localIpAddress(): String {
    try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { intf ->
            intf.inetAddresses?.toList()?.forEach { inetAddress ->
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: SocketException) {
        ex.printStackTrace()
    }
    throw IllegalArgumentException("No local IPv4 address found")
}


fun publicIpAddress(timeoutMs: Int = 5000): String? {
    val services = listOf(
        "https://api.ipify.org",
        "https://checkip.amazonaws.com",
        "https://ifconfig.me/ip"
    )

    for (service in services) {
        try {
            val url = URI(service).toURL()
            (url.openConnection() as? HttpURLConnection)?.run {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                inputStream.bufferedReader().use { reader ->
                    val ip = reader.readText().trim()
                    if (ip.isNotEmpty()) return ip
                }
            }
        } catch (e: Exception) {
        }
    }
    return null
}
