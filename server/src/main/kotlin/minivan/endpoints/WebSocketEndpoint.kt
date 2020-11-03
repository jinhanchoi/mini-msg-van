package minivan.endpoints

import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class WebSocketEndpoint : WebSocketHandler{
    val logger : org.slf4j.Logger? = org.slf4j.LoggerFactory.getLogger(WebSocketEndpoint::class.java)
    override fun handle(session: WebSocketSession): Mono<Void?>? {
        return session
                .send(session.receive()
                        .map { msg: WebSocketMessage ->
                            val now = LocalDateTime.now()
                            logger?.info(now.toString())
                            var sum = 0
                            for(i in 0 .. 1000000000){
                                //println(i)
                                sum += i
                            }

                            "RECEIVED ON SERVER :: " + msg.payloadAsText + sum
                        }
                        .map { payload: String? ->
                            val now = LocalDateTime.now()
                            logger?.info(now.toString())
                            session.textMessage(payload)
                        }
                )
    }
}
