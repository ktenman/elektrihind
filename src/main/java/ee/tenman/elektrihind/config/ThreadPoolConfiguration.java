package ee.tenman.elektrihind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfiguration {

    @Bean(name = "customExecutor")
    public Executor customExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(15);
	    executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CustomExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorService singleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public ExecutorService twoThreadExecutor() {
        return Executors.newFixedThreadPool(2);
    }

    @Bean
    public ExecutorService fourThreadExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    @Bean
    public ExecutorService tenThreadExecutor() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean
    public ExecutorService hundredThreadExecutor() {
        return Executors.newFixedThreadPool(100);
    }

    @Bean
    public ExecutorService xThreadExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
