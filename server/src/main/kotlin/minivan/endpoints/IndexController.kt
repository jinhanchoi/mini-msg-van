package minivan.endpoints

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.lang.UnsupportedOperationException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.logging.Logger

@RestController
@RequestMapping("/")
class IndexController {

    val logger : org.slf4j.Logger? = LoggerFactory.getLogger(IndexController::class.java)

    @Autowired
    @Qualifier("asyncExecutor")
    lateinit var executor : Executor

    @GetMapping("/index3")
    fun index2() : Mono<String>{
        for(i in 0 .. 1000000){
            //println(i)
            logger?.info("test")
        }
        return Mono.just("testtttt")
    }

    @GetMapping("/index2")
    fun home() : CompletableFuture<String> {

        val future : CompletableFuture<String> = CompletableFuture.supplyAsync({
            for(i in 0 .. 1000000){
                //println(i)
                logger?.info("test")
            }
            return@supplyAsync "home"
        },executor)
        logger?.info("{}","before return")
        return future
    }

    @GetMapping()
    fun index() : Mono<String> {
        val mono1 : Mono<List<String>> = Mono.just(listOf("1","2","3","4"))
        val mono2 : Mono<Collection<String>> = Mono.just(listOf("a","b","c","d","e"))
        Mono.zip(mono1, mono2).map { tuple -> print("${tuple.t1} and ${tuple.t2} ")}.subscribe()

        for(i in 0 .. 1000000){
            logger?.info("{}",i)
        }
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
    println("completed")
    val str = URL("https://www.google.com").getText()
    println(str)
    delay(5000)
    val google = getUrlText("https://www.google.com")
    val naver = getUrlText("https://www.naver.com")
    val daum = getUrlText("https://www.daum.net")

    runBlocking { listOf(google,naver,daum).map{ it.await()}.forEach { println(it) } }
}
suspend fun getUrlText(url:String) : Deferred<String> = coroutineScope{
    async{
        URL(url).getText()
    }
}

fun URL.getText(): String {
    return openConnection().run {
        this as HttpURLConnection
        inputStream.bufferedReader().readText()
    }
}
suspend fun myNameIs():String {
    println("I'm working in thread ${Thread.currentThread().name}")
    throw UnsupportedOperationException("Test exception")
}