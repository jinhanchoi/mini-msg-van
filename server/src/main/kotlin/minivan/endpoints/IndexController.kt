package minivan.endpoints

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/")
class IndexController {
    @GetMapping()
    fun index() : Mono<String> {

        val mono1 : Mono<List<String>> = Mono.just(listOf("1","2","3","4"))
        val mono2 : Mono<Collection<String>> = Mono.just(listOf("a","b","c","d","e"))
        Mono.zip(mono1, mono2).map { tuple -> print("${tuple.t1} and ${tuple.t2} ")}.subscribe()

        return Mono.just("hi index")
    }
}