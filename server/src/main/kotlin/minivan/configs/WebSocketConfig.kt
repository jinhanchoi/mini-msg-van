package minivan.configs

import minivan.endpoints.WebSocketEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import java.util.*


@Configuration
class WebSocketConfig {

    @Bean
    fun webSocketEndpoint(): WebSocketEndpoint {
        return WebSocketEndpoint()
    }

    @Bean
    fun handlerMapping(): HandlerMapping? {
        val map: MutableMap<String, WebSocketHandler> = HashMap<String, WebSocketHandler>()
        map["/echo"] = webSocketEndpoint()
        val mapping = SimpleUrlHandlerMapping()
        mapping.setUrlMap(map)
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE)
        return mapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter? {
        return WebSocketHandlerAdapter(webSocketService())
    }

    @Bean
    fun webSocketService(): WebSocketService? {
        return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
    }
}