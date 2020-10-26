package minivan.endpoints

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.lang.UnsupportedOperationException
import java.util.logging.Logger

@RestController
@RequestMapping("/")
class IndexController {
    val logger : org.slf4j.Logger? = LoggerFactory.getLogger(IndexController::class.java)

    @GetMapping()
    fun index() : Mono<String> {

        val mono1 : Mono<List<String>> = Mono.just(listOf("1","2","3","4"))
        val mono2 : Mono<Collection<String>> = Mono.just(listOf("a","b","c","d","e"))
        Mono.zip(mono1, mono2).map { tuple -> print("${tuple.t1} and ${tuple.t2} ")}.subscribe()

        val name : Mono<String> = Mono.fromSupplier{
            println("called")
            //throw RuntimeException("")
            "Jinhan"
        }

        val nameMono2 : Mono<String> = Mono.fromCallable{
            println("callable called")
            "test"
            //throw RuntimeException("test")
        }

        //callable called  and called printed
        nameMono2.and{name.subscribe()}.subscribe()
        val result : String  = nameMono2.`as` { str -> str.block()+"CONTATED" }
        println(result)

        return Mono.just("hi index")
    }

}

@InternalCoroutinesApi
fun main(args:Array<String>) = runBlocking {
    val dispatcher = newFixedThreadPoolContext(10,"MyContext")
    val scope = CoroutineScope(dispatcher)

    coroutineScope{

    }
    val test = scope.async {
        val job = this.coroutineContext[Job]
        println(job)
       myNameIs()
    }
    test.join()
    if(test.isCancelled){
        println("test")
        val exception = test.getCancellationException()
        println(exception.message)
    }
//    println("${test.join()} sss")
    println("completed")
//    test.join()
}

suspend fun myNameIs():String {
    println("I'm working in thread ${Thread.currentThread().name}")
    throw UnsupportedOperationException("Test exception")
}