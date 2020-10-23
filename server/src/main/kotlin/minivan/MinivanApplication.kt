package minivan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@EnableWebFlux
@SpringBootApplication
class MinivanApplication

fun main(args:Array<String>){
    runApplication<MinivanApplication>(*args)
}