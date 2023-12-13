package ee.tenman.elektrihind.config;

import ee.tenman.elektrihind.queue.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    public static final String ONE_DAY_CACHE_1 = "one-day-cache-11";
    public static final String ONE_DAY_CACHE_2 = "one-day-cache-22";
    public static final String ONE_DAY_CACHE_3 = "one-day-cache-33";
    public static final String ONE_DAY_CACHE_4 = "one-day-cache-4";
    public static final String ONE_YEAR_CACHE_1 = "one-year-cache-11";
    public static final String THIRTY_DAYS_CACHE_1 = "thirty-days-cache-11";
    public static final String MESSAGE_COUNTS_CACHE = "messageCounts-22";
    public static final String IMAGE_QUEUE = "image-queue";
    public static final String RESPONSE_QUEUE = "response-queue";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            RedisMessageSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(RESPONSE_QUEUE));

        return container;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ONE_DAY_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_2, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_3, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_4, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_YEAR_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(365)));
        cacheConfigurations.put(THIRTY_DAYS_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(30)));
        cacheConfigurations.put(MESSAGE_COUNTS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
