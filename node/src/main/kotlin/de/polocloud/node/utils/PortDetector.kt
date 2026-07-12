package de.polocloud.node.utils

import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceRepository
import de.polocloud.node.services.factory.platform.Platform
import java.net.InetSocketAddress
import java.net.ServerSocket

object PortDetector {

    const val SERVER_BASE_PORT = 30000
    const val PROXY_BASE_PORT = 25565

    fun nextPort(service: LocalService, platform: Platform): Int {
        var port = if (platform.type.equals("PROXY", ignoreCase = true)) PROXY_BASE_PORT else SERVER_BASE_PORT

        while (isPortUsed(service.nodeId, port)) {
            port += 1
        }
        return port
    }

    private fun isPortUsed(nodeId : String, port: Int): Boolean {
        for (service in  ServiceRepository.findAllForNode(nodeId)) {
            if (service.port == port) {
                return true
            }
        }
        try {
            ServerSocket().use { serverSocket ->
                serverSocket.bind(InetSocketAddress(port))
                return false
            }
        } catch (_: Exception) {
            return true
        }
    }
}