package com.nlpai4h.healthydemobacked.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 提供全局异步任务执行所需的线程池（如：AI生成报告异步任务）
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 核心线程数
        executor.setMaxPoolSize(10); // 最大线程数
        executor.setQueueCapacity(100); // 队列容量
        executor.setKeepAliveSeconds(60); // 线程活跃时间
        executor.setThreadNamePrefix("ai-gen-thread-"); // 线程名前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略：由调用者所在线程执行
        executor.setTaskDecorator(new ContextTaskDecorator()); // 设置上下文传递装饰器
        executor.initialize();
        return executor;
    }
}
