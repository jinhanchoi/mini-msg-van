package minivan.endpoints

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Controller
class HomeController{

    val logger : org.slf4j.Logger? = org.slf4j.LoggerFactory.getLogger(IndexController::class.java)

    @Autowired
    @Qualifier("asyncExecutor")
    lateinit var executor : Executor
    @GetMapping("/home")
    fun home() : CompletableFuture<String> {

        val future : CompletableFuture<String> = CompletableFuture.supplyAsync({
            println(Thread.currentThread().name)
            return@supplyAsync "home"
        },executor)
        logger?.info("{}","before return")
        return future
    }
}