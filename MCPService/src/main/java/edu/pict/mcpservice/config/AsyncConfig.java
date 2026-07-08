package edu.pict.mcpservice.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool configuration for MCPService.
 *
 * <p>Two isolated pools:
 *
 * <ul>
 *   <li><b>analysisExecutor</b> — handles per-UUID threat analysis offloaded from the Kafka
 *       listener thread. Sized for I/O-bound work (Redis + gRPC calls).
 *   <li><b>aiExecutor</b> — dedicated pool for AI inference calls so a slow/stalled AI service
 *       cannot starve the rule-based analysis threads.
 * </ul>
 */
@Configuration
public class AsyncConfig {

    /**
     * Primary analysis pool. Kafka listener submits one task per UUID here, freeing the consumer
     * thread to poll the next batch immediately.
     */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("mcp-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Isolated pool for AI inference calls. Keeps AI latency from blocking rule-based analysis
     * threads.
     */
    @Bean(name = "aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(128);
        executor.setThreadNamePrefix("mcp-ai-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
