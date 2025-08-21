package dev.httpmarco.polocloud.modules.rest.socket.web

import dev.httpmarco.polocloud.modules.rest.RestModule
import dev.httpmarco.polocloud.modules.rest.socket.BaseWebSocket
import dev.httpmarco.polocloud.modules.rest.socket.web.impl.v3.AliveWebSocket
import dev.httpmarco.polocloud.modules.rest.socket.web.impl.v3.PolocloudLogWebSocket

class WebSocketService {

    companion object {
        const val API_PATH = "/polocloud/api/v3"
    }

    private val authService = WebSocketAuthService()
    private val sockets = mutableListOf<BaseWebSocket>()

    init {
        register(
            AliveWebSocket(),
            PolocloudLogWebSocket()
        )

        this.sockets.forEach { socket ->
            RestModule.instance.httpServer.app.ws(API_PATH + socket.path) { ws ->
                ws.onConnect { context -> authService.authenticate(socket, context) }
                ws.onClose { socket::onClose }
                ws.onMessage { socket::onMessage }
                ws.onError { socket::onError }
            }
        }
    }

    private fun register(vararg webSockets: BaseWebSocket) {
        sockets += webSockets
    }

}