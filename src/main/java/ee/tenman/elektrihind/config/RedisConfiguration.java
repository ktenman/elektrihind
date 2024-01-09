package ee.tenman.elektrihind.config;

import ee.tenman.elektrihind.queue.redis.RedisMessageSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
public class RedisConfiguration {

    public static final String TEN_MINUTES = "ten-minutes";
    public static final String ONE_DAY_CACHE_1 = "one-day-cache-1";
    public static final String ONE_DAY_CACHE_2 = "one-day-cache-2";
    public static final String ONE_DAY_CACHE_3 = "one-day-cache-3";
    public static final String ONE_DAY_CACHE_4 = "one-day-cache-4";
    public static final String ONE_MONTH_CACHE_1 = "one-month_1";
    public static final String ONE_MONTH_CACHE_2 = "one-month_2";
    public static final String ONE_MONTH_CACHE_3 = "one-month_3";
    public static final String ONE_MONTH_CACHE_4 = "one-month_4";
    public static final String ONE_MONTH_CACHE_5 = "one-month_5";
    public static final String ONE_YEAR_CACHE_1 = "one-year-cache-1";
    public static final String ONE_YEAR_CACHE_2 = "one-year-cache-2";
    public static final String MESSAGE_COUNTS_CACHE = "message-counts-cache-1";
    public static final String SESSIONS_CACHE = "sessionsCache";
    public static final String APOLLO_KINO_CACHE = "apollo-kino";
    public static final String ELECTRICITY_PRICES_CACHE = "electricity-prices-cache";

    public static final String IMAGE_REQUEST_QUEUE = "image-request-queue";
    public static final String IMAGE_RESPONSE_QUEUE = "image-response-queue";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    @ConditionalOnProperty(name = "app.messaging.type", havingValue = "redis")
    RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            RedisMessageSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(IMAGE_RESPONSE_QUEUE));
        return container;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(TEN_MINUTES, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(ONE_DAY_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_2, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_3, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_DAY_CACHE_4, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_MONTH_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(43830)));
        cacheConfigurations.put(ONE_MONTH_CACHE_2, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(43830)));
        cacheConfigurations.put(ONE_MONTH_CACHE_3, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(43830)));
        cacheConfigurations.put(ONE_MONTH_CACHE_4, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(43830)));
        cacheConfigurations.put(ONE_MONTH_CACHE_5, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(43830)));
        cacheConfigurations.put(ONE_YEAR_CACHE_1, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(365)));
        cacheConfigurations.put(ONE_YEAR_CACHE_2, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(365)));
        cacheConfigurations.put(MESSAGE_COUNTS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(SESSIONS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(3)));
        cacheConfigurations.put(APOLLO_KINO_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(2)));
        cacheConfigurations.put(ELECTRICITY_PRICES_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(2)));
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
