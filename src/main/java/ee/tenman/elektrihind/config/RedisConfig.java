package ee.tenman.elektrihind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    public static final String ONE_HOUR_CACHE = "one-hour-cache";
    public static final String THIRTY_MINUTES_CACHE = "thirty-minutes-cache";
    public static final String ONE_DAY_CACHE = "one-day-cache";
    public static final String MESSAGE_COUNTS_CACHE = "messageCounts";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ONE_HOUR_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(THIRTY_MINUTES_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(ONE_DAY_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(MESSAGE_COUNTS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
