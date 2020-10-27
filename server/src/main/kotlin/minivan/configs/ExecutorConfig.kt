package minivan.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class ExecutorConfig {
    @Bean
    fun asyncExecutor() : Executor{
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 10
        executor.setQueueCapacity(500)
        executor.threadNamePrefix = "TaskExecutor- "
        executor.initialize()
        return executor
    }
}