package com.nextalex.seckill.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolExecutorConfig {

    // 核心线程数
    @Value("${thread-pool.core-pool-size:2}")
    private int corePoolSize;

    // 最大线程
    @Value("${thread-pool.max-pool-size:5}")
    private int maxPoolSize;

    // 任务队列长度
    @Value("${thread-pool.queue-capacity:100}")
    private int queueCapacity;

    // 线程空闲存活时间
    @Value("${thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean("bizExecutor")
    public Executor bizExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setQueueCapacity(queueCapacity);
        threadPoolTaskExecutor.setKeepAliveSeconds(keepAliveSeconds);

        threadPoolTaskExecutor.setThreadNamePrefix("biz-task-");
        // 拒绝策略，提交任务线程执行任务
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 任务执行完毕才关闭线程池
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待超时
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
        // 线程池初始化
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }


}
