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

    public static final String ONE_HOUR_CACHE = "one-hour-cache-2";
    public static final String THIRTY_MINUTES_CACHE = "thirty-minutes-cache-2";
    public static final String ONE_DAY_CACHE = "one-day-cache-3";
    public static final String ONE_YEAR_CACHE = "one-year-cach-2";
    public static final String MESSAGE_COUNTS_CACHE = "messageCounts-2";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ONE_HOUR_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(THIRTY_MINUTES_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(ONE_DAY_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put(ONE_YEAR_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(365)));
        cacheConfigurations.put(MESSAGE_COUNTS_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(1)));
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
