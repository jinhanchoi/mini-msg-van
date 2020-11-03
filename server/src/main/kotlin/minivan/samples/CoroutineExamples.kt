package minivan.samples

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
fun main(args:Array<String>) = runBlocking {
    val dispatcher = newFixedThreadPoolContext(10,"MyContext")
    val scope = CoroutineScope(dispatcher)
    println("finish")

    val test = object : CoroutineScope{
        override val coroutineContext: CoroutineContext
            get() = EmptyCoroutineContext
    }
    scope.launch{
        println("normal")
    }.invokeOnCompletion { cause -> when(cause){
            null -> println("completed")
            else -> println(cause.message)
        }
    }

    scope.onMessage(listOf(1,2,3,455))
    scope.launch(context = buildExceptionHandler(), start=CoroutineStart.DEFAULT){
        TODO("fortest")
    }.invokeOnCompletion { cause -> when(cause){
        null -> println("completed")
        else -> println(cause.message)
    }
    }
    val job = scope.launch(start = CoroutineStart.LAZY) {
        println("ttt")
    }
//    print("end")
    job.join()
    println(job.isCancelled)
    println("test")
    GlobalScope.launch { println("global") }

    scope.launch{
        println("normal")
    }.invokeOnCompletion { cause ->
        cause?.let { println(it.message) }
    }


    println("completed")
//    val str = URL("https://www.google.com").readText()
//    println(str)
//    delay(5000)
    val google = getUrlText("https://www.google.com")
    val naver = getUrlText("https://www.naver.com")
    val daum = getUrlText("https://www.daum.net")
    val feeds = listOf(google,naver,daum)
    feeds.forEach { it.await() }
    val merged = feeds.joinToString { it.getCompleted() }
    delay(1000)
    val googlestr = google.getCompleted()
    println(merged)
}

suspend fun getUrlText(url:String) : Deferred<String> = coroutineScope{
    async{
        val result = URL(url).readText()
        println(result.length)
        return@async result
    }
}

//fun URL.getText(): String {
//    return openConnection().run {
//        this as HttpURLConnection
//        inputStream.bufferedReader().readText()
//    }
//}
suspend fun testRun()= coroutineScope {
    onMessage(listOf(1,2,3,455))
}

fun buildExceptionHandler() = CoroutineExceptionHandler{
    _: CoroutineContext, throwable: Throwable ->
        println("Job cancelled due to  ${throwable}")
}

fun buildExceptionHandler2() : CoroutineExceptionHandler {
    return CoroutineExceptionHandler{
        _: CoroutineContext, throwable: Throwable ->
        println("Job cancelled due to  ${throwable}")
    }
}
//코루틴 스콥을 사용자가 정할수 있게 확장 함수로 정의함.
fun CoroutineScope.onMessage(ids: List<Int>): List<Job> {
    return ids.map { id ->
        // launch is called on "this", which is the coroutineScope.
        launch { println(id)}
    }
}
